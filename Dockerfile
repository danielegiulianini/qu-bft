FROM alpine:latest
#hseeberger/scala-sbt:11.0.13_1.6.2_2.13.8

# Copies everything from ’my - app /’ (on the host ) to ’/my -app /’
#on the container file system
#COPY <src>... <dest>
COPY ./ /qu/
# Moves to the /my - app / directory on the container file system
WORKDIR /qu/
# Compiles the to -be - deployed app
RUN sbt compile

# Sets some env . var. on the container
ENV SBT_OPTS ""
ENV DEMO_MAIN_PATH "qu.model.Demo"

# Run the application by default
#CMD ./ gradlew run $GRD_OPTS
CMD sbt "run-main $DEMO_MAIN_PATH"
