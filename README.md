# DevUmgApp

Aplicación móvil Android para la Universidad Mariano Gálvez de Guatemala (UMG).

Cliente móvil de la API `sgau-backend-api`: la app consumirá los servicios del backend
(autenticación, gestión académica y demás funcionalidades) una vez integrada la capa de red.

## Estado actual

- Pantalla de login implementada (UI y validación local).
- Pendiente: integración con `sgau-backend-api` mediante Retrofit, autenticación con
  token y navegación entre pantallas.

## Stack

- Lenguaje: Java 11
- Build: Gradle 9.4.1 (wrapper incluido)
- Android: minSdk 29 (Android 10), targetSdk 36
- Librerías: AndroidX (AppCompat, ConstraintLayout, CardView) y Material Components

## Flujo de ramas

- `main`: producción. Solo recibe merges de `develop` cuando hay una versión lista.
- `develop`: integración. Todas las ramas de trabajo salen de acá.
- `feature/<nombre>`: una rama por funcionalidad.

Flujo de trabajo:

```
feature/<nombre> --> develop --> main
```

## Compilar y probar

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/`.

## Requisitos

- JDK 17 o superior
- Android SDK (la ruta local se configura en `local.properties`, archivo que no se versiona)
