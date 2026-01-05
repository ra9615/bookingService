package com.example.booking.client;

import com.example.booking.config.FeignConfig;
import com.example.booking.dto.ConfirmAvailabilityRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "hotel-service", configuration = FeignConfig.class)
public interface HotelClient {

    @PostMapping("/rooms/{id}/confirm")
    void confirmAvailability(@PathVariable("id") Long roomId, @RequestBody ConfirmAvailabilityRequestDto request);

    @PostMapping("/rooms/{id}/release")
    void releaseRoom(@PathVariable("id") Long roomId, @RequestParam("requestId") String requestId);
}

