@echo off
title Sync ke GitHub - Orion Navigator
echo ========================================================
echo   MENYINKRONKAN PERUBAHAN KE GITHUB
echo ========================================================
cd /d "%~dp0"

git status --short

echo.
set /p commit_msg="Masukkan keterangan perubahan (tekan Enter untuk default 'Update project'): "
if "%commit_msg%"=="" set commit_msg=Update project

echo.
echo [1/3] Menandai semua perubahan...
git add .

echo [2/3] Menyimpan commit...
git commit -m "%commit_msg%"

echo [3/3] Mengunggah (push) ke GitHub...
git push origin main

echo.
echo ========================================================
echo   SELESAI! Repo GitHub Anda sudah terupdate.
echo ========================================================
pause
