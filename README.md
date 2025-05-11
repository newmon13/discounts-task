# Discounts Calculator

![Java](https://img.shields.io/badge/Java-21-brightgreen)

## Table of Contents

1. [About](#about)
2. [Features](#features)
3. [Technologies](#technologies)
4. [Getting Started](#getting-started)

## About

Program uses a greedy approach to maximize discount benefits. The application processes orders by value (highest to lowest) and selects best payment methods.
The algorithm runs twice with different priorities - first favoring cards over partial point, then in reverse. Better solution is picked.


## Technologies

- Java 21
- Maven
- JUnit & Mockito

## Getting Started

```bash
mvn install
cd target
java -jar app.jar <absolute_orders_path> <absolute_payment_methods_path>
```

### Prerequisites

- Java 21 






