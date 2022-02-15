./gradlew clean build
java -jar "build/libs/Shnap-0.0.12-fat.jar" -arg="shnap_src/compiled/builtins" -compile="shnap_src/stdlib/builtins" -archive="std_builtins"
java -jar "build/libs/Shnap-0.0.12-fat.jar" -arg="shnap_src/compiled/lib" -compile="shnap_src/stdlib/lib" -archive="std_lib"
java -jar "build/libs/Shnap-0.0.12-fat.jar" -arg="shnap_src/compiled/natives" -compile="shnap_src/stdlib/natives" -archive="std_natives"
java -jar "build/libs/Shnap-0.0.12-fat.jar" -arg="shnap_src/compiled/prelib" -compile="shnap_src/stdlib/prelib" -archive="std_prelib"
./gradlew zipCompiled
./gradlew clean build