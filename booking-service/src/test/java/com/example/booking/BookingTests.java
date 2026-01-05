package com.example.booking;

import com.example.booking.dto.CreateBookingRequestDto;
import com.example.booking.model.Booking;
import com.example.booking.repository.BookingRepository;
import com.example.booking.service.BookingService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@ContextConfiguration(initializers = BookingTests.WireMockConfig.class)
public class BookingTests {

    static class WireMockConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static final WireMockServer wireMockServer = new WireMockServer(0);

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            wireMockServer.start();
            int port = wireMockServer.port();

            TestPropertyValues.of(
                    "hotel.base-url=http://localhost:" + port,
                    "hotel.timeout-ms=1000",
                    "hotel.retries=1"
            ).applyTo(context.getEnvironment());
        }
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void resetWireMockServer() {
        WireMockConfig.wireMockServer.resetAll();
    }

    @AfterAll
    static void shutdownWireMock() {
        WireMockConfig.wireMockServer.stop();
    }

    @Test
    void testSuccessfulBookingConfirmation() {
        stubFor(post(urlPathMatching("/rooms/\\d+/hold")).willReturn(okJson("{}")));
        stubFor(post(urlPathMatching("/rooms/\\d+/confirm")).willReturn(okJson("{}")));

        CreateBookingRequestDto requestDto = new CreateBookingRequestDto(10L, LocalDate.now(), LocalDate.now().plusDays(1), "r1");
        Booking booking = bookingService.create(1L, requestDto);
        Assertions.assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
    }

    @Test
    void testBookingFailureWithCompensation() {
        stubFor(post(urlPathMatching("/rooms/\\d+/hold")).willReturn(serverError()));
        stubFor(post(urlPathMatching("/rooms/\\d+/release")).willReturn(okJson("{}")));

        CreateBookingRequestDto requestDto = new CreateBookingRequestDto(11L, LocalDate.now(), LocalDate.now().plusDays(1), "r2");

        Booking booking = bookingService.create(2L, requestDto);
        Assertions.assertEquals(Booking.Status.CANCELLED, booking.getStatus());
    }

    @Test
    void testBookingTimeoutHandling() {
        stubFor(post(urlPathMatching("/rooms/\\d+/hold"))
                .willReturn(aResponse().withFixedDelay(2000).withStatus(200)));
        stubFor(post(urlPathMatching("/rooms/\\d+/release")).willReturn(okJson("{}")));

        CreateBookingRequestDto requestDto = new CreateBookingRequestDto(12L, LocalDate.now(), LocalDate.now().plusDays(1), "r3");

        Booking booking = bookingService.create(3L, requestDto);
        Assertions.assertEquals(Booking.Status.CANCELLED, booking.getStatus());
    }

    @Test
    void testBookingIdempotency() {
        stubFor(post(urlPathMatching("/rooms/\\d+/hold")).willReturn(okJson("{}")));
        stubFor(post(urlPathMatching("/rooms/\\d+/confirm")).willReturn(okJson("{}")));

        CreateBookingRequestDto requestDto = new CreateBookingRequestDto(13L, LocalDate.now(), LocalDate.now().plusDays(1), "r4");


        Booking firstBooking = bookingService.create(4L, requestDto);
        Booking secondBooking = bookingService.create(4L, requestDto);

        Assertions.assertEquals(firstBooking.getId(), secondBooking.getId());
    }
}
