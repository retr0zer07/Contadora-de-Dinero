# Contadora de Dinero

App Android para contar dinero por denominaciones.

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

## Versión web (opcional)
En **Settings → Pages**, elige *Source: GitHub Actions*. La app quedará en
`https://<usuario>.github.io/Contadora-de-Dinero/` y se puede instalar desde Chrome con "Agregar a pantalla principal".

## Desarrollo local
Abre `www/index.html` en el navegador. Para Android Studio:
```
npm install
npx cap add android
npx cap open android
```