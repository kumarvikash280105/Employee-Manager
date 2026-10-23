@echo off
echo ===================================================================
echo   EmpManage - Starting Spring Boot CMS Backend on Port 8090
echo   Database: PostgreSQL (empmanage_cms_db : 5432)
echo ===================================================================
echo.

if exist "target\empmanage-cms-1.0.0.jar" (
    java -jar target\empmanage-cms-1.0.0.jar
) else (
    echo Building and launching via Maven...
    mvn spring-boot:run
)

pause
