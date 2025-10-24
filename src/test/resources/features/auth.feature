Feature: Autenticación y gestión de usuarios (Reto 2)
  Como cliente del API
  Quiero validar registro, login y recursos protegidos
  Para asegurar el correcto funcionamiento del microservicio

  Background:
    Given la base URL del Auth API

  @registro-exitoso
  Scenario: Registro exitoso de usuario nuevo
    When hago POST a "/api/users" con JSON:
      """
      {
        "username": "${REGISTER_USERNAME}_${RAND}",
        "email": "${REGISTER_EMAIL}",
        "password": "${REGISTER_PASSWORD}",
        "phoneNumber": "${REGISTER_PHONE}"
      }
      """
    Then la respuesta debe tener status 200
    And el header "Content-Type" contiene "application/json"
    And el JSON cumple el schema "schemas/user-response-schema.json"

  @login-exitoso
  Scenario: Login exitoso con credenciales válidas
    When hago POST a "/api/auth/login" con JSON:
      """
      {
        "username": "${LOGIN_USERNAME}",
        "password": "${LOGIN_PASSWORD}"
      }
      """
    Then la respuesta debe tener status 200
    And guardo el token JWT retornado como "AUTH_TOKEN"
    # (login devuelve texto plano; no aplica schema JSON)

  @usuarios-autorizado
  Scenario: Acceso autorizado al listado de usuarios
    Given tengo un token válido (login previo si es necesario)
    When hago GET a "/api/users" con bearer "${AUTH_TOKEN}"
    Then la respuesta debe tener status 200
    And el header "Content-Type" contiene "application/json"
    And el JSON cumple el schema "schemas/user-page-schema.json"

  @usuarios-sin-token
  Scenario: Acceso denegado sin token
    When hago GET a "/api/users" sin autorización
    Then la respuesta debe tener status 401
    And el header "Content-Type" contiene "application/json"
    And el JSON cumple el schema "schemas/error-schema.json"
