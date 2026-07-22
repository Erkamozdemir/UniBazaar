package com.unibazaar.dtos;
public record CategoryDTO(int id, String name, Integer parentId) {
    @Override
    public String toString() { return name; }
}
