package com.example.dockercompile.psmodule;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class PythonProblemService {

    private final PythonDockerExecutor pythonDockerExecutor = new PythonDockerExecutor();

    public void marking(PythonCode pythonCode) {

        List<String> resultList = pythonDockerExecutor.executeAllCode(pythonCode.getExecutableCodeList());
        for (String result : resultList) {
            String[] split = result.split("\n");
            System.out.println("result = " + split[0]);
//            System.out.println("result = " + result);
        }
    }

    public static void main(String[] args) {
        List<String> inputList = new ArrayList<>();
        inputList.add("1, 2");
        inputList.add("3, 4");

        List<String> resultList = new ArrayList<>();
        resultList.add("3");
        resultList.add("7");

        PythonCode pythonCode = new PythonCode("def solution(num1, num2):\n\treturn num1+num2", inputList, resultList);


        for (String execCode : pythonCode.getExecutableCodeList()) {
            log.info("실행할 코드 \n{}\n======================", execCode);
        }

        PythonProblemService pythonProblemService = new PythonProblemService();
        pythonProblemService.marking(pythonCode);
    }
}
