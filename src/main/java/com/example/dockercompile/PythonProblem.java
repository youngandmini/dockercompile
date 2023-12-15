package com.example.dockercompile;


public class PythonProblem {

    public static String codeOf(String function) {
        return function +
                "\nprint(solution(1, 2))" +
                "\nprint(solution(2, 3))";
    }
}
