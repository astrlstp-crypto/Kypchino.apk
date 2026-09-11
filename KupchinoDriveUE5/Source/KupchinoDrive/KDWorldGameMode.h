#pragma once

#include "CoreMinimal.h"
#include "GameFramework/GameModeBase.h"
#include "KDWorldGameMode.generated.h"

UCLASS()
class KUPCHINODRIVE_API AKDWorldGameMode : public AGameModeBase
{
    GENERATED_BODY()

public:
    AKDWorldGameMode();
    virtual void BeginPlay() override;
    virtual void Tick(float DeltaSeconds) override;

    void GenerateMission();
    void ToggleFreeMode();

    FVector GetMissionTarget() const { return MissionTarget; }
    bool IsFreeMode() const { return bFreeMode; }
    int32 GetMoney() const { return Money; }
    FString GetRegionName() const { return RegionName; }

private:
    FVector MissionTarget = FVector::ZeroVector;
    bool bFreeMode = false;
    int32 Money = 0;
    float ParkTimer = 0.f;
    FString RegionName = TEXT("Kupchino");

    TArray<FVector> ParkingSpots;

    void BuildPrototypeWorld();
    void SpawnBox(const FVector& Location, const FVector& Scale);
};
