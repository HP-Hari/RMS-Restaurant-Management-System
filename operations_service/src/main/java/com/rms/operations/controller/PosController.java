package com.rms.operations.controller;

import com.rms.operations.model.Order;
import com.rms.operations.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class PosController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/pos")
    public String posDashboard() {
        return "pos"; // Renders pos.html
    }

    @PostMapping("/pos/checkout")
    public String checkout(@RequestParam("tableId") String tableId, 
                           @RequestParam("totalAmount") Double totalAmount) {
        Order newOrder = new Order();
        try {
            newOrder.setTable_number(Integer.parseInt(tableId));
        } catch (NumberFormatException e) {
            newOrder.setTable_number(null);
        }
        newOrder.setTotal_amount(totalAmount);
        newOrder.setStatus("PENDING");
        newOrder.setCreated_at(LocalDateTime.now());
        
        orderRepository.save(newOrder);
        
        // Redirect back to POS and clear cart (in a real app, maybe show a success toast)
        return "redirect:/pos?success=true";
    }
}
