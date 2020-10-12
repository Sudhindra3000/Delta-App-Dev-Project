package com.sudhindra.deltaappdevproject.viewmodels;

import androidx.lifecycle.ViewModel;

import com.sudhindra.deltaappdevproject.utils.Student;

public class SignUpViewModel extends ViewModel {

    private Student student;

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
}
