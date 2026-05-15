package com.example.catalog.repository;

import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import com.example.catalog.repository.spec.ReviewSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User author;
    private Movie movie;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .name("Critic")
                .email("critic@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .blocked(false)
                .build();
        entityManager.persist(author);

        movie = Movie.builder()
                .title("Test Movie")
                .build();
        entityManager.persist(movie);

        Review published = Review.builder()
                .author(author)
                .movie(movie)
                .score(8)
                .content("Отличный фильм")
                .status(ReviewStatus.PUBLISHED)
                .build();
        Review spam = Review.builder()
                .author(author)
                .movie(movie)
                .score(1)
                .content("spam ссылка")
                .status(ReviewStatus.SPAM)
                .flagged(true)
                .build();
        Review deleted = Review.builder()
                .author(author)
                .movie(movie)
                .score(5)
                .content("Удалено модератором")
                .status(ReviewStatus.DELETED)
                .build();
        entityManager.persist(published);
        entityManager.persist(spam);
        entityManager.persist(deleted);
        entityManager.flush();
    }

    @Test
    void shouldFilterByStatusAndFlagged() {
        Specification<Review> base = ReviewSpecifications.baseFilters("spam", movie.getId(), null, null);
        Specification<Review> spec = ReviewSpecifications.withStatus(base, ReviewStatus.SPAM);

        Page<Review> page = reviewRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(ReviewStatus.SPAM);
    }
}
