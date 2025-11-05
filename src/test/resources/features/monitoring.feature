@monitoring
Feature: API del servicio de monitoreo

  Background:
    Given el servicio de monitoreo está disponible

  @smoke
  Scenario: Registrar y listar servicios
    When registro para monitoreo el servicio "auth-service"
    Then listar la salud global retorna arreglo

  Scenario: Consultar por nombre
    When registro para monitoreo el servicio "orchestrator"
    And consulto la salud por nombre "orchestrator"
