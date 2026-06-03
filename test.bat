@echo off
echo.
echo ========================================
echo  Ejecutando pruebas: lista-espera-service
echo ========================================
cd lista-espera-service
call mvnw.cmd test
start "" "%CD%\target\site\jacoco\index.html"
cd ..

echo.
echo ========================================
echo  Ejecutando pruebas: paciente-service
echo ========================================
cd paciente-service
call mvnw.cmd test
start "" "%CD%\target\site\jacoco\index.html"
cd ..

echo.
echo ========================================
echo  Ejecutando pruebas: medico-service
echo ========================================
cd medico-service
call mvnw.cmd test
start "" "%CD%\target\site\jacoco\index.html"
cd ..

echo.
echo ========================================
echo  Pruebas finalizadas - reportes abiertos
echo ========================================
pause
