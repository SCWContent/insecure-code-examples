package sqlinjection.domain;

public class TodoItem {

    Integer id;
    String description;
    Integer status;

    public TodoItem(Integer id, String description, Integer status){
        this.id = id;
        this.description = description;
        this.status = status;
    }
}
