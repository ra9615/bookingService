package com.example.hotel.service;

import com.example.hotel.model.Hotel;
import com.example.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;

    public List<Hotel> listHotels() {
        return hotelRepository.findAll();
    }

    public Optional<Hotel> getHotel(Long id) {
        return hotelRepository.findById(id);
    }

    public Hotel saveHotel(Hotel h) {
        return hotelRepository.save(h);
    }

    public void deleteHotel(Long id) {
        hotelRepository.deleteById(id);
    }
}


