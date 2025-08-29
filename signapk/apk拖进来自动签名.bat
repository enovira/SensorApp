@echo off
if "%~1"=="" echo,put ttf file &pause&exit /b
java -jar signapk.jar  platform.x509.pem  platform.pk8 "%~1" "%~n1_sign.apk"