package io.github.evan0x.syllacal.term;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

public class Term {

    private Long id;

    @NotBlank(message = "Term name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    public Term() {
    }

    public Term(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean overlaps(Term other) {
        throw new UnsupportedOperationException("Implement Term.overlaps() in the domain model.");
    }

    public boolean contains(LocalDate date) {
        throw new UnsupportedOperationException("Implement Term.contains() in the domain model.");
    }

    public long getDurationInDays() {
        throw new UnsupportedOperationException("Implement Term.getDurationInDays() in the domain model.");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Term term = (Term) o;
        return Objects.equals(id, term.id)
                && Objects.equals(name, term.name)
                && Objects.equals(startDate, term.startDate)
                && Objects.equals(endDate, term.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startDate, endDate);
    }

    @Override
    public String toString() {
        return "Term{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
