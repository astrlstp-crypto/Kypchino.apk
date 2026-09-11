#include "KDWorldGameMode.h"
#include "KDCarPawn.h"
#include "KDHUD.h"
#include "Engine/StaticMeshActor.h"
#include "Components/StaticMeshComponent.h"
#include "Kismet/GameplayStatics.h"

AKDWorldGameMode::AKDWorldGameMode()
{
    PrimaryActorTick.bCanEverTick = true;
    DefaultPawnClass = AKDCarPawn::StaticClass();
    HUDClass = AKDHUD::StaticClass();
}

void AKDWorldGameMode::BeginPlay()
{
    Super::BeginPlay();
    BuildPrototypeWorld();

    ParkingSpots = {
        FVector(1800, 1200, 95), FVector(-2200, 900, 95), FVector(3200, -1700, 95),
        FVector(7200, 900, 95), FVector(9400, -1400, 95), FVector(12500, 1700, 95),
        FVector(-7000, -900, 95), FVector(-9800, 1500, 95), FVector(-12500, -1400, 95)
    };

    GenerateMission();
}

void AKDWorldGameMode::SpawnBox(const FVector& Location, const FVector& Scale)
{
    UStaticMesh* Cube = LoadObject<UStaticMesh>(nullptr, TEXT("/Engine/BasicShapes/Cube.Cube"));
    if (!Cube) return;

    AStaticMeshActor* A = GetWorld()->SpawnActor<AStaticMeshActor>(Location, FRotator::ZeroRotator);
    if (!A) return;

    A->GetStaticMeshComponent()->SetStaticMesh(Cube);
    A->SetActorScale3D(Scale);
    A->GetStaticMeshComponent()->SetMobility(EComponentMobility::Static);
}

void AKDWorldGameMode::BuildPrototypeWorld()
{
    // Three city/country zones connected by one long highway.
    SpawnBox(FVector(0,0,-60), FVector(160,160,0.5f));
    SpawnBox(FVector(0,0,0), FVector(150,10,0.12f));
    SpawnBox(FVector(0,0,0), FVector(10,150,0.12f));

    const TArray<float> Centers = { -10000.f, 0.f, 10000.f };
    for (float CX : Centers)
    {
        for (int32 X=-3; X<=3; ++X)
        {
            for (int32 Y=-3; Y<=3; ++Y)
            {
                if (FMath::Abs(Y) <= 1 || FMath::Abs(X) <= 1) continue;
                const float Height = 2.0f + ((FMath::Abs(X*13 + Y*7) % 7) * 0.65f);
                SpawnBox(FVector(CX + X*900.f, Y*900.f, Height*50.f), FVector(3.0f,3.0f,Height));
            }
        }
    }
}

void AKDWorldGameMode::GenerateMission()
{
    if (ParkingSpots.Num() == 0) return;
    bFreeMode = false;
    ParkTimer = 0.f;
    MissionTarget = ParkingSpots[FMath::RandRange(0, ParkingSpots.Num()-1)];

    if (MissionTarget.X < -5000) RegionName = TEXT("Nordland");
    else if (MissionTarget.X > 5000) RegionName = TEXT("Southport");
    else RegionName = TEXT("Kupchino");
}

void AKDWorldGameMode::ToggleFreeMode()
{
    bFreeMode = !bFreeMode;
    ParkTimer = 0.f;
}

void AKDWorldGameMode::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);
    if (bFreeMode) return;

    APlayerController* PC = UGameplayStatics::GetPlayerController(this, 0);
    AKDCarPawn* Car = PC ? Cast<AKDCarPawn>(PC->GetPawn()) : nullptr;
    if (!Car) return;

    const float Dist = FVector::Dist2D(Car->GetActorLocation(), MissionTarget);
    if (Dist < 360.f && FMath::Abs(Car->GetSpeed()) < 120.f)
    {
        ParkTimer += DeltaSeconds;
        if (ParkTimer >= 1.5f)
        {
            Money += 500;
            GenerateMission();
        }
    }
    else
    {
        ParkTimer = 0.f;
    }
}
