package me.fndo.fnclient;

/**
 * 测试用 User 类
 */
public class TestUser {
    private Long id;
    private String name;

    public TestUser() {
    }

    public TestUser(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
