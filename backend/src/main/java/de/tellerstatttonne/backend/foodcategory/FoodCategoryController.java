package de.tellerstatttonne.backend.foodcategory;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/food-categories")
public class FoodCategoryController {

    private final FoodCategoryService service;

    public FoodCategoryController(FoodCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<FoodCategory> listActive() {
        return service.findAllActive();
    }
}
