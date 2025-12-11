"""
app.py - API mejorada para guardar usuarios, notas de voz (audios) y media (imágenes/videos)
Guarda metadatos en SQLite y opcionalmente almacena archivos como BLOB en la BD además de mantener copia en disco.
Lenguaje: Español en comentarios y respuestas JSON.

Requisitos:
- flask
- flask_sqlalchemy
- werkzeug

Instalar (si no lo tienes):
pip install flask flask_sqlalchemy
"""
import os
import uuid
import io
from datetime import datetime
from typing import Optional

from flask import Flask, request, jsonify, send_from_directory, send_file, abort
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from sqlalchemy import or_

# --- CONFIGURACIÓN ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_FOLDER = os.path.join(BASE_DIR, 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Cambia a True si quieres guardar archivo BINARIO dentro de la DB además de la copia en disco.
STORE_FILES_IN_DB = True

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(BASE_DIR, 'grandline.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY', 'tu_clave_secreta_aqui')

db = SQLAlchemy(app)

# --- MODELOS ---
class User(db.Model):
    __tablename__ = 'user'
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(120), unique=True, nullable=False)
    username = db.Column(db.String(80), nullable=False)
    password_hash = db.Column(db.String(256), nullable=False)

    # Archivo en disco (ruta), y opcionalmente en DB como BLOB
    profile_picture_path = db.Column(db.String(256), nullable=True)
    profile_picture_filename = db.Column(db.String(256), nullable=True)
    profile_picture_mimetype = db.Column(db.String(128), nullable=True)
    profile_picture_data = db.Column(db.LargeBinary, nullable=True)  # Si STORE_FILES_IN_DB=True, aquí se guarda

    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def set_password(self, password: str):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        return check_password_hash(self.password_hash, password)

    def to_dict(self, host='192.168.2.2', port=5000):
        profile_url = None
        if self.profile_picture_path:
            # Preferir endpoint que sirva desde disco
            profile_url = f'http://{host}:{port}/uploads/{os.path.basename(self.profile_picture_path)}'
        return {
            'id': self.id,
            'email': self.email,
            'username': self.username,
            'profile_picture_url': profile_url,
            'created_at': self.created_at.isoformat()
        }


class AudioMessage(db.Model):
    __tablename__ = 'audio_message'
    id = db.Column(db.Integer, primary_key=True)
    sender_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    receiver_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)

    # Archivo de audio: ruta en disco y opcional BLOB + metadata
    audio_path = db.Column(db.String(256), nullable=False)
    audio_filename = db.Column(db.String(256), nullable=False)
    audio_mimetype = db.Column(db.String(128), nullable=True)
    audio_data = db.Column(db.LargeBinary, nullable=True)

    # Archivo media opcional (imagen/video): ruta en disco y opcional BLOB + metadata
    media_path = db.Column(db.String(256), nullable=True)
    media_filename = db.Column(db.String(256), nullable=True)
    media_mimetype = db.Column(db.String(128), nullable=True)
    media_data = db.Column(db.LargeBinary, nullable=True)

    text_note = db.Column(db.Text, nullable=True)  # Mensaje textual opcional
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

    sender = db.relationship('User', foreign_keys=[sender_id], backref='sent_messages')
    receiver = db.relationship('User', foreign_keys=[receiver_id], backref='received_messages')

    def to_dict(self, host='192.168.2.2', port=5000):
        audio_url = f'http://{host}:{port}/media/audio/{self.id}'
        media_url = f'http://{host}:{port}/media/media/{self.id}' if self.media_path or self.media_data else None
        return {
            'id': self.id,
            'sender_id': self.sender_id,
            'receiver_id': self.receiver_id,
            'text_note': self.text_note,
            'audio_url': audio_url,
            'media_url': media_url,
            'timestamp': self.timestamp.isoformat()
        }


# --- UTILIDADES ---
ALLOWED_EXTENSIONS = {'mp3', 'm4a', 'aac', 'wav', 'ogg', 'mp4', 'avi', 'mov', 'jpg', 'jpeg', 'png', 'gif'}

def allowed_file(filename: Optional[str]) -> bool:
    if not filename:
        return False
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def save_file_to_disk(file_storage, subfolder: str = '') -> (str, str, str):
    """Guarda archivo en disco y devuelve (file_path, filename, mimetype)."""
    filename = str(uuid.uuid4()) + '_' + secure_filename(file_storage.filename)
    folder = app.config['UPLOAD_FOLDER']
    if subfolder:
        folder = os.path.join(folder, subfolder)
        os.makedirs(folder, exist_ok=True)
    file_path = os.path.join(folder, filename)
    file_storage.save(file_path)
    return file_path, filename, file_storage.mimetype

def read_file_bytes(file_storage) -> bytes:
    file_storage.stream.seek(0)
    return file_storage.read()

# --- RUTAS PARA SERVIR ARCHIVOS EN DISCO ---
@app.route('/uploads/<path:filename>', methods=['GET'])
def uploaded_file(filename):
    """Sirve archivos desde la carpeta uploads/ (copia en disco)."""
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename, as_attachment=False)

# Endpoints para servir media desde DB o disco de forma transparente
@app.route('/media/audio/<int:message_id>', methods=['GET'])
def get_audio_by_message(message_id):
    msg = AudioMessage.query.get(message_id)
    if not msg:
        return jsonify({'message': 'Mensaje no encontrado'}), 404

    # Si se guardó en DB y hay datos, servir desde BLOB
    if STORE_FILES_IN_DB and msg.audio_data:
        return send_file(io.BytesIO(msg.audio_data),
                         mimetype=msg.audio_mimetype or 'application/octet-stream',
                         download_name=msg.audio_filename,
                         as_attachment=False)
    # Fallback: servir desde disco si existe
    if msg.audio_path and os.path.exists(msg.audio_path):
        return send_file(msg.audio_path, mimetype=msg.audio_mimetype or None)
    return jsonify({'message': 'Archivo de audio no disponible'}), 404

@app.route('/media/media/<int:message_id>', methods=['GET'])
def get_media_by_message(message_id):
    msg = AudioMessage.query.get(message_id)
    if not msg:
        return jsonify({'message': 'Mensaje no encontrado'}), 404

    if STORE_FILES_IN_DB and msg.media_data:
        return send_file(io.BytesIO(msg.media_data),
                         mimetype=msg.media_mimetype or 'application/octet-stream',
                         download_name=msg.media_filename,
                         as_attachment=False)
    if msg.media_path and os.path.exists(msg.media_path):
        return send_file(msg.media_path, mimetype=msg.media_mimetype or None)
    return jsonify({'message': 'Archivo media no disponible'}), 404

# --- ENDPOINTS PRINCIPALES ---

@app.route('/register', methods=['POST'])
def register():
    """
    Registra un nuevo usuario.
    Campos multipart/form-data:
     - email (required)
     - username (required)
     - password (required)
     - profile_picture (optional, file)
    """
    data = request.form
    email = data.get('email')
    username = data.get('username')
    password = data.get('password')

    if not all([email, username, password]):
        return jsonify({'message': 'Faltan campos (email, username, password)'}), 400

    if User.query.filter_by(email=email).first():
        return jsonify({'message': 'El correo ya está registrado'}), 409

    new_user = User(email=email, username=username)
    new_user.set_password(password)

    # Profile picture opcional
    if 'profile_picture' in request.files:
        file = request.files['profile_picture']
        if file and allowed_file(file.filename):
            file_path, filename, mimetype = save_file_to_disk(file, subfolder='profiles')
            new_user.profile_picture_path = file_path
            new_user.profile_picture_filename = filename
            new_user.profile_picture_mimetype = mimetype
            if STORE_FILES_IN_DB:
                new_user.profile_picture_data = read_file_bytes(file)

    db.session.add(new_user)
    db.session.commit()

    return jsonify({'message': 'Usuario registrado exitosamente', 'user': new_user.to_dict()}), 201

@app.route('/login', methods=['POST'])
def login():
    """
    Login simple. JSON body:
    { "email": "...", "password": "..." }
    """
    data = request.get_json(silent=True)
    if not data:
        return jsonify({'message': 'Petición inválida, se esperaba JSON'}), 400

    email = data.get('email')
    password = data.get('password')

    if not all([email, password]):
        return jsonify({'message': 'Faltan campos (email, password)'}), 400

    user = User.query.filter_by(email=email).first()
    if user and user.check_password(password):
        return jsonify({'message': 'Inicio de sesión exitoso', 'user': user.to_dict()}), 200
    return jsonify({'message': 'Credenciales inválidas'}), 401

@app.route('/users', methods=['GET'])
def get_all_users():
    users = User.query.all()
    return jsonify([u.to_dict() for u in users]), 200

@app.route('/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({'message': 'Usuario no encontrado'}), 404
    return jsonify(user.to_dict()), 200

@app.route('/messages', methods=['POST'])
def send_message():
    """
    Enviar mensaje (nota de voz obligatoria).
    multipart/form-data:
     - sender_id (required)
     - receiver_id (required)
     - text_note (optional)
     - audio_file (required, file)
     - media_file (optional, file)
    """
    # Validar IDs
    form = request.form
    try:
        sender_id = int(form.get('sender_id')) if form.get('sender_id') is not None else None
        receiver_id = int(form.get('receiver_id')) if form.get('receiver_id') is not None else None
    except ValueError:
        return jsonify({'message': 'sender_id y receiver_id deben ser enteros'}), 400

    if not all([sender_id, receiver_id]):
        return jsonify({'message': 'Faltan sender_id o receiver_id'}), 400

    # Validar que usuarios existan
    sender = User.query.get(sender_id)
    receiver = User.query.get(receiver_id)
    if not sender or not receiver:
        return jsonify({'message': 'sender_id o receiver_id no válidos'}), 404

    # Audio obligatorio
    if 'audio_file' not in request.files:
        return jsonify({'message': 'No se encontró el archivo de audio'}), 400

    audio_file = request.files['audio_file']
    if audio_file.filename == '' or not allowed_file(audio_file.filename):
        return jsonify({'message': 'Audio inválido o sin nombre de archivo'}), 400

    audio_path, audio_filename, audio_mimetype = save_file_to_disk(audio_file, subfolder='audios')

    media_path = None
    media_filename = None
    media_mimetype = None
    media_data = None

    if 'media_file' in request.files:
        media_file = request.files['media_file']
        if media_file.filename != '' and allowed_file(media_file.filename):
            media_path, media_filename, media_mimetype = save_file_to_disk(media_file, subfolder='media')
            if STORE_FILES_IN_DB:
                media_file.stream.seek(0)
                media_data = media_file.read()

    # Leer audio bytes si queremos guardarlo en BD
    audio_blob = None
    if STORE_FILES_IN_DB:
        audio_file.stream.seek(0)
        audio_blob = audio_file.read()

    text_note = form.get('text_note')

    new_message = AudioMessage(
        sender_id=sender_id,
        receiver_id=receiver_id,
        audio_path=audio_path,
        audio_filename=audio_filename,
        audio_mimetype=audio_mimetype,
        audio_data=audio_blob,
        media_path=media_path,
        media_filename=media_filename,
        media_mimetype=media_mimetype,
        media_data=media_data,
        text_note=text_note
    )

    db.session.add(new_message)
    db.session.commit()

    return jsonify({'message': 'Mensaje enviado exitosamente', 'message_data': new_message.to_dict()}), 201

@app.route('/messages/<int:user1_id>/<int:user2_id>', methods=['GET'])
def get_chat_messages(user1_id, user2_id):
    """Obtener mensajes entre dos usuarios ordenados por timestamp ascendente."""
    messages = AudioMessage.query.filter(
        or_(
            (AudioMessage.sender_id == user1_id) & (AudioMessage.receiver_id == user2_id),
            (AudioMessage.sender_id == user2_id) & (AudioMessage.receiver_id == user1_id)
        )
    ).order_by(AudioMessage.timestamp).all()
    return jsonify([m.to_dict() for m in messages]), 200

@app.route('/messages/<int:message_id>', methods=['DELETE'])
def delete_message(message_id):
    """Eliminar un mensaje y sus archivos (tanto disco como BLOB en la BD)."""
    msg = AudioMessage.query.get(message_id)
    if not msg:
        return jsonify({'message': 'Mensaje no encontrado'}), 404

    # Eliminar archivos en disco si existen
    try:
        if msg.audio_path and os.path.exists(msg.audio_path):
            os.remove(msg.audio_path)
        if msg.media_path and os.path.exists(msg.media_path):
            os.remove(msg.media_path)
    except Exception:
        # No bloquear por error al eliminar archivos físicos
        pass

    db.session.delete(msg)
    db.session.commit()
    return jsonify({'message': f'Mensaje {message_id} eliminado exitosamente'}), 200

# --- INICIALIZACIÓN DE LA BD Y ARRANQUE ---
def create_tables():
    """Crea las tablas si no existen (usar app_context() para mayor compatibilidad)."""
    db.create_all()

if __name__ == '__main__':
    # Crear tablas usando el contexto de la app (evita hooks dependientes de la versión)
    with app.app_context():
        create_tables()

    # Configuración de host/puerto. Cambia la IP si necesitas exponerlo en otra interfaz.
    host = os.environ.get('API_HOST', '192.168.2.2')
    port = int(os.environ.get('API_PORT', 5000))
    debug = os.environ.get('FLASK_DEBUG', 'True').lower() in ('1', 'true', 'yes')

    print(f"Arrancando servidor en http://{host}:{port}  (STORE_FILES_IN_DB={STORE_FILES_IN_DB})")
    app.run(host=host, port=port, debug=debug)