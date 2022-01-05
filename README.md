# docker-patch

##install
使用maven package，得到"docker-patch-1.0-SNAPSHOT.jar"

mvn package, you can get "docker-patch-1.0-SNAPSHOT.jar"

##Usage
```aidl
java -jar docker-patch-1.0-SNAPSHOT.jar -d /home/docker-img-v1.tar /home/docker-img-v2.tar
```
"-d": create a diff between docker-img-v1.tar(absolute path) and docker-img-v2.tar, and generate a patch.

```aidl
java -jar docker-patch-1.0-SNAPSHOT.jar -m /home/docker-img-v1.tar /home/docker-patch-20220104235120.tar
```
"-m": you can generate docker-img-v2.tar from docker-img-v1.tar and a patch
