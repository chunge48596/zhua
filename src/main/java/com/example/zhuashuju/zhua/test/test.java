package com.example.zhuashuju.zhua.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class test {
    private static final Logger log = LoggerFactory.getLogger(test.class);
    public static void main(String[] args) {
        List<Student> a1 = new ArrayList<>();
        Student s1 = new Student(1, "张三", 11);
        Student s2 = new Student(2, "李四", 13);
        Student s3 = new Student(3, "王五", 15);
        Student s4 = new Student(4, "赵柳", 18);
        a1.add(s1);
        a1.add(s2);
        a1.add(s3);
        a1.add(s4);

        List<StudentDTO> a2 = new ArrayList<>();
        StudentDTO s11 = new StudentDTO(3, "张三", 10);
        StudentDTO s21 = new StudentDTO(4, "李四", 13);
        StudentDTO s31 = new StudentDTO(7, "王五", 15);
        StudentDTO s41 = new StudentDTO(8, "赵柳", 18);
        a2.add(s31);
        a2.add(s21);
        a2.add(s11);
        a2.add(s41);

        // 根据id 从 a1、a2 取Student交集
        List<Student> listStudent = a1.stream().filter(student -> a2.stream().anyMatch(studentDTO -> studentDTO.getId()==student.getId())).collect(Collectors.toList());

        // a1 - listStudent
        List<Student> a1_list1 = a1.stream().filter(item -> !listStudent.contains(item)).collect(Collectors.toList());

        // 根据id 从 a1、a2 取StudentDTO交集
        List<StudentDTO> listStudentDTO = a2.stream().filter(studentDto -> a1.stream().anyMatch(student -> student.getId()==studentDto.getId())).collect(Collectors.toList());

        // a2 - listStudentDTO
        List<StudentDTO> a2_list2 = a2.stream().filter(item -> !listStudentDTO.contains(item)).collect(Collectors.toList());

        //List<String> reduce2 = list2.stream().filter(item -> !list1.contains(item)).collect(toList());

        log.debug("aaaaaaaaaaaaaaaaa");
    }
}
