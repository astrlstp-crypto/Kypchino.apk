#include "KDCarPawn.h"
#include "KDWorldGameMode.h"
#include "Camera/CameraComponent.h"
#include "Components/StaticMeshComponent.h"
#include "GameFramework/SpringArmComponent.h"
#include "Kismet/GameplayStatics.h"
#include "UObject/ConstructorHelpers.h"

AKDCarPawn::AKDCarPawn()
{
    PrimaryActorTick.bCanEverTick = true;

    Body = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("Body"));
    RootComponent = Body;
    Body->SetSimulatePhysics(false);
    Body->SetCollisionProfileName(TEXT("Pawn"));

    static ConstructorHelpers::FObjectFinder<UStaticMesh> Cube(TEXT("/Engine/BasicShapes/Cube.Cube"));
    if (Cube.Succeeded())
    {
        Body->SetStaticMesh(Cube.Object);
        Body->SetRelativeScale3D(FVector(2.2f, 1.1f, 0.55f));
    }

    CameraBoom = CreateDefaultSubobject<USpringArmComponent>(TEXT("CameraBoom"));
    CameraBoom->SetupAttachment(RootComponent);
    CameraBoom->TargetArmLength = 650.f;
    CameraBoom->SetRelativeLocation(FVector(0, 0, 170));
    CameraBoom->SetRelativeRotation(FRotator(-14.f, 0.f, 0.f));
    CameraBoom->bEnableCameraLag = true;
    CameraBoom->CameraLagSpeed = 6.f;

    Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
    Camera->SetupAttachment(CameraBoom);

    AutoPossessPlayer = EAutoReceiveInput::Player0;
}

void AKDCarPawn::MoveForward(float Value) { Throttle = FMath::Clamp(Value, -1.f, 1.f); }
void AKDCarPawn::MoveRight(float Value) { Steering = FMath::Clamp(Value, -1.f, 1.f); }

void AKDCarPawn::ToggleMode()
{
    if (AKDWorldGameMode* GM = Cast<AKDWorldGameMode>(UGameplayStatics::GetGameMode(this)))
        GM->ToggleFreeMode();
}

void AKDCarPawn::NewMission()
{
    if (AKDWorldGameMode* GM = Cast<AKDWorldGameMode>(UGameplayStatics::GetGameMode(this)))
        GM->GenerateMission();
}

void AKDCarPawn::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);

    const float Accel = 1800.f;
    const float MaxForward = 2400.f;
    const float MaxReverse = 900.f;
    const float Friction = 1050.f;

    if (FMath::Abs(Throttle) > 0.01f)
    {
        CurrentSpeed += Throttle * Accel * DeltaSeconds;
    }
    else
    {
        CurrentSpeed = FMath::FInterpConstantTo(CurrentSpeed, 0.f, DeltaSeconds, Friction);
    }

    CurrentSpeed = FMath::Clamp(CurrentSpeed, -MaxReverse, MaxForward);

    const float SpeedAlpha = FMath::Clamp(FMath::Abs(CurrentSpeed) / MaxForward, 0.f, 1.f);
    if (SpeedAlpha > 0.02f)
    {
        const float DirectionSign = CurrentSpeed >= 0.f ? 1.f : -1.f;
        AddActorLocalRotation(FRotator(0.f, Steering * DirectionSign * (32.f + 42.f * SpeedAlpha) * DeltaSeconds, 0.f));
    }

    FHitResult Hit;
    AddActorLocalOffset(FVector(CurrentSpeed * DeltaSeconds, 0.f, 0.f), true, &Hit);
    if (Hit.IsValidBlockingHit())
        CurrentSpeed *= -0.18f;

    FVector L = GetActorLocation();
    L.Z = 90.f;
    SetActorLocation(L);

    if (L.Z < -1000.f)
    {
        SetActorLocation(FVector::ZeroVector);
        CurrentSpeed = 0.f;
    }
}

void AKDCarPawn::SetupPlayerInputComponent(UInputComponent* PlayerInputComponent)
{
    Super::SetupPlayerInputComponent(PlayerInputComponent);
    PlayerInputComponent->BindAxis(TEXT("MoveForward"), this, &AKDCarPawn::MoveForward);
    PlayerInputComponent->BindAxis(TEXT("MoveRight"), this, &AKDCarPawn::MoveRight);
    PlayerInputComponent->BindAction(TEXT("ToggleMode"), IE_Pressed, this, &AKDCarPawn::ToggleMode);
    PlayerInputComponent->BindAction(TEXT("NewMission"), IE_Pressed, this, &AKDCarPawn::NewMission);
}
