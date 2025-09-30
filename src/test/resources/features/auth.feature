Feature: Auth API

  Scenario: Registrar y hacer login
    Given el servicio de auth está disponible
    When registro un usuario válido
    Then la respuesta al registrar debe ser 200
    When intento login con el usuario
    Then la respuesta al login debe ser 200 y debe contener un token
