FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests \
	&& mv target/futslot-api-*.jar target/app.jar \
	&& java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted
# A Java runtime with only the modules the API uses (jdeps), plus English and Nepali locale data
RUN jdeps --ignore-missing-deps -q --recursive --multi-release 25 --print-module-deps \
		--class-path 'extracted/dependencies/lib/*' extracted/application/app.jar > modules \
	&& jlink --add-modules "$(cat modules),jdk.localedata,jdk.management" --include-locales=en,ne \
		--strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /jre

FROM alpine:3.22
RUN adduser -S -u 1001 futslot && mkdir -p /data/media && chown futslot /data/media
COPY --from=build /jre /opt/java
ENV PATH=/opt/java/bin:$PATH
WORKDIR /app
# Dependencies change least, so they come first and stay cached between releases
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
USER futslot
EXPOSE 8080
# Sized for a 1 GB server shared with other apps: a small heap, the serial collector (no GC threads),
# the quick JIT only, and smaller thread stacks. Override with JAVA_TOOL_OPTIONS.
ENV JAVA_TOOL_OPTIONS="-Xmx192m -Xss512k -XX:+UseSerialGC -XX:+UseCompactObjectHeaders -XX:TieredStopAtLevel=1 \
-XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=32m"
ENTRYPOINT ["java", "-jar", "app.jar"]
