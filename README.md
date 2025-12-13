# GrandLine

> **Proyecto Integrador - Desarrollo de Aplicaciones Móviles**
>
> **Semestre:** [Cuatrimestre/Grupo]
> **Fecha de entrega:** 11 de Diciembre

Repositorio: https://github.com/AvilaCamacho/GrandLine  
Repo ID: 1114529269

---

## Equipo de Desarrollo

| Nombre Completo | Rol / Tareas Principales | Usuario GitHub |
| :--- | :--- | :--- |
| [ANGEL GILBERTO AVILA LOPEZ] | [BACK-END, SERVERVER CONECCION] | @usuario1 |
| [ROBERTO CARLOS ESPINOZA JIMEEZ] | [FRONT-END] | @Robertin2102 |
| [MIGUEL EMILIANO BASILIO GARDEA] | [DOCUMENTACION] | @Emliano520 |

---

## Descripción del Proyecto

**¿Qué hace la aplicación?**  AP
APP TIPO WASAP QUE PERMITE TENER CHATS CON USUARIOS REGISTRADOS, AL DAR CLICK EN UN USUARIO ABRIRAA UN CHAT PERSONAL ENTRE LOS DOS USUARIOS QUE PERMITE PRIVACIDAD ENTRE CHATS, SOLO PUEDE ENVIAR MENSAJE DE VOZ EN LOS CHATS TANTO
TANTO QUIEN LO ENVIA COMO QUIEN LO RECIBE PUEDE ESCUCHAR EL AUDIO
**Objetivo:**  
Demostrar la implementación de una arquitectura robusta en Android utilizando servicios hardware del dispositivo.
-USO DE MICROFONO
## Stack Tecnológico y Características
-PHYTON SERVER PARA LA API
Este proyecto ha sido desarrollado siguiendo estrictamente los lineamientos de la materia:
- **Lenguaje:** Kotlin 100%.
- **Interfaz de Usuario:** Jetpack Compose.
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **Conectividad (API REST):** Retrofit.
  - **GET:** [TIPO MEDIA, NOTAS DE VOZ EN FORMATO MP3]
  - **POST:** [NOTAA DE VOZ MP3]
  - **UPDATE:** [CHAT SE ACTULIZA CADA SEGUNDO PARA MOSTAR EL AUDIO]
  - **DELETE:** [BORRAR FOTO DE PERFIL EL USUARIUO PUEDE HACER ESO]
- **Sensor Integrado:** [MICROFONO]
  - Uso: [PERMITE AL USUARIO GRABAR LA NOTA DE VOZ QUE SE ENVIARA POR EL CHAT A SU CONTACTO]
- **Persistencia local:** [EL REGISTRO DEL CHAT Y SUS NOTAS DE VOZ ENVIADAS SE GUARDAN LOCALMENTE Y SE ENVIAN IGUAL AL SERVIDOR]
- **Gestión de dependencias:** [Gradle / Koin / Hilt — describe si aplica]
---
## Flujo de la Aplicación (breve)
1. Pantalla de inicio / autenticación (si aplica).
2. Lista / consulta de recursos vía API (GET).
3. Crear / editar / eliminar recursos (POST / PUT / DELETE).
4. Uso del sensor para [funcionalidad concreta].
5. Almacenamiento local y sincronización con servidor.
---




## Capturas de Pantalla
Coloca al menos 3 imágenes en la carpeta `/docs` o en el README con rutas relativas o URLs públicas.
| Pantalla de Inicio | Operación CRUD | Uso del Sensor |
| :---: | :---: | :---: |
| ![) | ![CRUD](docs/screenshot_crud.png) | ![Sensor](docs/screenshot_sensor.png) |
(Actualiza las rutas e imágenes según las capturas reales del proyecto.)
---

## Instalación y Releases

El ejecutable firmado (.apk) se encuentra disponible en la sección de **Releases** de este repositorio.

1. Ve a la sección "Releases" (o haz clic [aquí](https://github.com/AvilaCamacho/GrandLine/releases)).
2. Descarga el archivo `.apk` de la última versión.
3. Instálalo en tu dispositivo Android (asegúrate de permitir la instalación de orígenes desconocidos).

---

## Estructura del Proyecto (sugerida)

- app/
  - src/main/java/... (paquetes)
  - src/main/res/
  - AndroidManifest.xml
- data/ (modelos, repositorios)
- ui/ (composables, screens)
- di/ (dependencias, Hilt/Koin)
- docs/ (capturas, diagramas)
- build.gradle (configuración)

---

## Cómo contribuir

1. Haz fork del repositorio.
2. Crea una rama feature/tu-feature.
3. Abre un Pull Request describiendo los cambios.
4. Ejecuta pruebas y documenta cualquier cambio relevante.

---

## Notas finales

- Completa los campos entre corchetes [] con la información específica del proyecto (objetivos, endpoints, sensor, capturas, miembros).
- Si necesitas, puedo generar una versión final del README rellenando los campos si me proporcionas:
  - Descripción corta de la aplicación.
  - Lista de integrantes y sus GitHub handles.
  - Endpoints usados (GET/POST/PUT/DELETE) con ejemplos.
  - Sensor usado y su propósito.
  - Rutas o URLs de capturas de pantalla.
  - Link a la release / apk (si ya existe).
