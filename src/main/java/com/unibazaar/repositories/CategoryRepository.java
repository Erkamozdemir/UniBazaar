package com.unibazaar.repositories;

import com.unibazaar.dtos.CategoryDTO;
import java.util.List;

public interface CategoryRepository {
    List<CategoryDTO> findAll();
}
