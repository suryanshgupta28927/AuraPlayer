exec java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
chmod +x gradlew && git add gradlew && git commit -m "add gradlew" && git push origin main
