-- V3: Add indexes for range and composite search queries on properties
CREATE INDEX idx_properties_rent_amount ON properties(rent_amount);
CREATE INDEX idx_properties_available_city_rent ON properties(available, city, rent_amount);
