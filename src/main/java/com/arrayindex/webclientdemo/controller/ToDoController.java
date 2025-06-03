package com.arrayindex.webclientdemo.controller;

import com.arrayindex.webclientdemo.service.ToDoService;
import com.arrayindex.webclientdemo.domain.ToDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/todos")
public class ToDoController {
  @Autowired
  ToDoService toDoService;

  @GetMapping("/{id}")
  public Mono<ToDo> getTodo(@PathVariable String id) {
    return toDoService.getToDoById(id);
  }

  @GetMapping("")
  public Flux<ToDo> getTodos() {
    return toDoService.getToDos();
  }
}
