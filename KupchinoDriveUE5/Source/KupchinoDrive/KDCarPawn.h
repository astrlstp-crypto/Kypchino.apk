#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Pawn.h"
#include "KDCarPawn.generated.h"

class UStaticMeshComponent;
class USpringArmComponent;
class UCameraComponent;

UCLASS()
class KUPCHINODRIVE_API AKDCarPawn : public APawn
{
    GENERATED_BODY()

public:
    AKDCarPawn();
    virtual void Tick(float DeltaSeconds) override;
    virtual void SetupPlayerInputComponent(UInputComponent* PlayerInputComponent) override;

    float GetSpeed() const { return CurrentSpeed; }

private:
    UPROPERTY(VisibleAnywhere) UStaticMeshComponent* Body;
    UPROPERTY(VisibleAnywhere) USpringArmComponent* CameraBoom;
    UPROPERTY(VisibleAnywhere) UCameraComponent* Camera;

    float Throttle = 0.f;
    float Steering = 0.f;
    float CurrentSpeed = 0.f;

    void MoveForward(float Value);
    void MoveRight(float Value);
    void ToggleMode();
    void NewMission();
};
