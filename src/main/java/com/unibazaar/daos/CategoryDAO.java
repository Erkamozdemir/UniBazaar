package com.unibazaar.daos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.CategoryDTO;
import com.unibazaar.repositories.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

public class CategoryDAO implements CategoryRepository {
    private final SupabaseClient client = SupabaseClient.getInstance();
    private final Gson gson = new Gson();

    public List<CategoryDTO> findAll() {
        String response = client.get("categories?select=id,name,parent_id");
        JsonArray array = gson.fromJson(response, JsonArray.class);
        List<CategoryDTO> categories = new ArrayList<>();
        if (array == null) return categories;
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            categories.add(new CategoryDTO(
                obj.get("id").getAsInt(),
                obj.get("name").getAsString(),
                obj.get("parent_id").isJsonNull() ? null : obj.get("parent_id").getAsInt()
            ));
        }
        return categories;
    }
}
