package com.example.dockercompile;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
public class DockerPython {

    public static void main(String[] args) {

        //어떤 코드를 실행시킬 것인가?
        String pythonCode = "print(\"Hello Python with Docker in Java!\")";
//        String pythonCode = "print(input())";
        String functionDefinitionSum = "def solution(num1, num2):" +
                "\treturn num1+num2";
        String functionDefinitionMul = "def solution(num1, num2):" +
                "\treturn num1*num2";
        String functionDefinitionMin = "def solution(num1, num2):" +
                "\treturn num1-num2";
//        String pythonCode = PythonProblem.codeOf(functionDefinitionSum);



        // 도커 설정
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        // 도커 HTTP 클라이언트
        // 도커는 HTTP로 통신한다
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(1)
                .connectionTimeout(Duration.ofSeconds(30L))
                .responseTimeout(Duration.ofSeconds(45L))
                .build();

        // 도커 클라이언트 얻기
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);


        // 도커 클라이언트에서 파이썬 이미지 땡겨오기
        try {
            dockerClient
                    .pullImageCmd("python:3.9.18-alpine3.18")
                    .exec(new PullImageResultCallback()).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 도커 컨테이너 생성하여 파이썬 코드 실행
        CreateContainerResponse container = dockerClient
                .createContainerCmd("python:3.9.18-alpine3.18")
                .withAttachStdin(true)
                .withCmd("python", "-i", "-c", pythonCode)
                .exec();
        //echo "Hello, World!" | docker run -i python:3.8-alpine python -c "print(input())"

        System.out.println("container.getId() = " + container.getId());

        dockerClient.startContainerCmd(container.getId()).exec();

//        String inputData = "Hello, World!";
//        InputStream inputStream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
//        dockerClient.execStartCmd(container.getId())
//                .withStdIn(inputStream)
//                .exec(new ExecStartResultCallback());

        StringBuilder resultLogBuilder = new StringBuilder();
        // 결과 로그
        try {
            dockerClient.logContainerCmd(container.getId())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallbackTemplate<ResultCallback.Adapter<Frame>, Frame>() {

                        @Override
                        public void onNext(Frame object) {
                            // 각 로그 프레임마다 호출되는 콜백
                            String resultLine = new String(object.getPayload());
//                            System.out.print(resultLine);
                            resultLogBuilder.append(resultLine);
                        }
                    }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("resultLog 출력");
        System.out.println(resultLogBuilder);
        System.out.println("resultLog 출력 종료");

        //도커 종료
        try {
            dockerClient.stopContainerCmd(container.getId()).exec();
        } catch (NotModifiedException e) {
            System.out.println("already closed");
        }
        dockerClient.removeContainerCmd(container.getId()).exec();
    }

}
