package com.example.dockercompile.psmodule;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PythonCode {

    private String functionDefinition;
    private List<String> inputList;
    private List<String> answerList;

    public PythonCode(String functionDefinition, List<String> inputList, List<String> answerList) {
        this.functionDefinition = functionDefinition;
        this.inputList = inputList;
        this.answerList = answerList;
    }

    public List<String> getExecutableCodeList() {

        List<String> executableCodeList = new ArrayList<>();
        for (String input : inputList) {
            String codeBuilder = functionDefinition +
                    "\nprint(solution(" +
                    input +
                    "))";
            executableCodeList.add(codeBuilder);
        }
        return executableCodeList;
    }

    public boolean compare(List<String> resultList) {
        return answerList.equals(resultList);
    }
}
