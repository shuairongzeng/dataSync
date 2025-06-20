@echo off
echo Starting DbSync Backend Application...

REM 设置Java类路径
set CLASSPATH=target\classes

REM 添加所有依赖jar包到类路径
for %%i in (target\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i

REM 如果没有lib目录，尝试使用Maven依赖
if not exist target\lib (
    echo Maven dependencies not found in target\lib
    echo Please run: mvn dependency:copy-dependencies -DoutputDirectory=target/lib
    echo Or use IDE to run the application
    pause
    exit /b 1
)

REM 启动应用
echo Starting application with classpath: %CLASSPATH%
java -cp "%CLASSPATH%" com.dbsync.dbsync.DbsyncApplication

pause
