# Contadora de Dinero

App Android nativa (Kotlin + Jetpack Compose) para contar dinero por denominaciones.

## Funciones
- Escribe la cantidad de piezas por denominación (o usa los botones − / +) y obtén subtotales y total al instante.
- Contador de piezas, billetes y monedas.
- Historial de cuentas con nombre/nota: ver detalle, volver a cargar en el contador o eliminar.
- Marca con checkbox qué denominaciones se muestran al contar.
- Crea tus propias denominaciones (billete o moneda, cualquier valor) y cambia el símbolo de la moneda.
- Funciona sin conexión; los datos se guardan en el dispositivo.

## Obtener el APK
1. Haz push a `main`. En **Actions** → *Android APK* → descarga el artefacto `ContadoraDeDinero-apk`.
2. Para una versión pública, crea un tag:
   ```
   git tag v1.0.0
   git push origin v1.0.0
   ```
   El APK aparecerá en **Releases**.
3. En el teléfono, abre el APK y permite "instalar apps de origen desconocido".

## Desarrollo local
Abre la carpeta del proyecto en Android Studio y ejecuta en un emulador o teléfono.
Código fuente en `app/src/main/java/com/contadora/dinero/`.