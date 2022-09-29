## [usual](usual)

常规测试
[TestApplication](usual/src/main/kotlin/top/abosen/geektime/tdd/intro/usual/TestApplication.kt)
在入口函数直接进行测试应用

[StudentRepositoryTest](usual/src/test/kotlin/top/abosen/geektime/tdd/intro/usual/StudentRepositoryTest.kt)
将 TestApplication 改写为自动化测试

## [run-it](run-it)

不会使用HK2，这里直接硬编码创建`StudentRepository`
[WebService](run-it/src/main/kotlin/top/abosen/geektime/tdd/intro/runit/WebService.kt)
启动embed web container
[students.http](run-it/src/main/resources/students.http) 用于进行外部http测试