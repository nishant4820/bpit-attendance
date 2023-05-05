
# 📝 Attendance Management System App

This is an Android app for managing attendance records of students by faculty. It allows faculty to log in, view their assigned subjects, take attendance, and submit attendance records. Additionally, the app provides features for viewing statistics month-wise, editing the last attendance, and assign new subjects in the beginning of new semester.

## 📲 Installation

This app is not available on the Google Play Store as of now and can only be installed manually from the APK file.

## 🔑 Usage

1. Log in to the system using your username and password.
2. Once logged in, you will see a list of your assigned subjects.
3. Click on a subject to view a list of students in that class.
4. Take attendance by marking the students as present or absent.
5. Submit the attendance record to save it to the system.
6. To view statistics month-wise, navigate to the statistics section in the app.
7. To edit the last attendance, select the attendance record from the list and make the necessary changes.
8. If the option to assign new subjects is enabled from the backend, you will be able to assign new subjects/classes from within the app.
9. View your profile by clicking on the profile section in the app.

## 🚀 Features

- Login, logout, change password, reset password functionality.
- View assigned subjects and take attendance for each subject.
- View statistics month-wise.
- Edit the last attendance record.
- Ability to assign new subjects if enabled from the backend.
- User's authorization token is stored using SharedPreferences.
- Includes a profile section.

## 🛠️ Technologies Used

- Kotlin for Android app development.
- OkHttp library for making API calls to the backend.
- SharedPreferences for storing user's authorization token.

## 💻 Credits

This app was developed by Nishant Mittal as part of a project for Bhagwan Parshuram Institute of Technology, Delhi.

## 📝 Note

All data is fetched by API calls from the backend, and a local database is not present. However, future plans include adding a local database using Room to make the app work offline as well and taking the app on the Google Play Store.

We hope you find this project useful! 😄
