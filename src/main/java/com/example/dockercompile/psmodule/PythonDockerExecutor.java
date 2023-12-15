package com.example.dockercompile.psmodule;

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
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PythonDockerExecutor {

    private DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    private final String PYTHON_VERSION = "python:3.9.18-alpine3.18";

    public List<String> executeAllCode(List<String> executableCodeList) {
        List<String> resultList = new ArrayList<>();
        for (String executableCode : executableCodeList) {
            String result = exec(executableCode);
            resultList.add(result);
        }
        return resultList;
    }

    public String exec(String executableCode) {

        DockerClient dockerClient = getDockerClient();


        // 도커 클라이언트에서 파이썬 이미지 풀링
        try {
            dockerClient
                    .pullImageCmd(PYTHON_VERSION)
                    .exec(new PullImageResultCallback()).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 도커 컨테이너 생성하여 파이썬 코드 실행
        CreateContainerResponse container = dockerClient
                .createContainerCmd(PYTHON_VERSION)
                .withAttachStdin(true)
                .withCmd("python", "-c", executableCode)
                .exec();

        log.info("도커 컨테이너 생성. container id = {}", container.getId());

        dockerClient.startContainerCmd(container.getId()).exec();

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
                            resultLogBuilder.append(resultLine);
                        }
                    }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        removeDockerClient(dockerClient, container);

        return resultLogBuilder.toString();
    }


    private DockerClient getDockerClient() {
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
        return dockerClient;
    }

    //도커 종료
    private static void removeDockerClient(DockerClient dockerClient, CreateContainerResponse container) {
        try {
            dockerClient.stopContainerCmd(container.getId()).exec();
        } catch (NotModifiedException e) {
            log.debug("Already Closed Container: {}", container.getId());
        }
        dockerClient.removeContainerCmd(container.getId()).exec();
    }
}
