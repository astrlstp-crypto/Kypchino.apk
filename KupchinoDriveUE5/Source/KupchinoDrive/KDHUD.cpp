#include "KDHUD.h"
#include "KDWorldGameMode.h"
#include "KDCarPawn.h"
#include "Engine/Canvas.h"
#include "Kismet/GameplayStatics.h"

void AKDHUD::DrawHUD()
{
    Super::DrawHUD();
    if (!Canvas) return;

    AKDWorldGameMode* GM = Cast<AKDWorldGameMode>(UGameplayStatics::GetGameMode(this));
    APlayerController* PC = GetOwningPlayerController();
    AKDCarPawn* Car = PC ? Cast<AKDCarPawn>(PC->GetPawn()) : nullptr;
    if (!GM || !Car) return;

    const float W = Canvas->SizeX;
    const float H = Canvas->SizeY;

    DrawText(FString::Printf(TEXT("KUPCHINO DRIVE   $%d"), GM->GetMoney()), FLinearColor::White, 30, 30, nullptr, 1.25f);
    DrawText(GM->IsFreeMode() ? TEXT("FREE DRIVE") : FString::Printf(TEXT("MISSION: PARK IN %s"), *GM->GetRegionName()),
             GM->IsFreeMode() ? FLinearColor::Green : FLinearColor::Yellow, 30, 65, nullptr, 1.0f);
    DrawText(TEXT("W/S drive  A/D steer  M free mode  N new mission"), FLinearColor(0.8f,0.8f,0.8f,1), 30, 95, nullptr, .8f);

    const float MapSize = 220.f;
    const float MX = W - MapSize - 25.f;
    const float MY = 25.f;
    DrawRect(FLinearColor(0.02f,0.02f,0.02f,0.75f), MX, MY, MapSize, MapSize);

    auto ToMap = [&](const FVector& P)->FVector2D
    {
        const float Scale = MapSize / 30000.f;
        return FVector2D(MX + MapSize*0.5f + P.X*Scale, MY + MapSize*0.5f + P.Y*Scale);
    };

    FVector2D P = ToMap(Car->GetActorLocation());
    DrawRect(FLinearColor::Cyan, P.X-4, P.Y-4, 8, 8);

    if (!GM->IsFreeMode())
    {
        FVector2D T = ToMap(GM->GetMissionTarget());
        DrawRect(FLinearColor::Yellow, T.X-6, T.Y-6, 12, 12);
        const float D = FVector::Dist2D(Car->GetActorLocation(), GM->GetMissionTarget());
        DrawText(FString::Printf(TEXT("Parking: %.0f m"), D/100.f), FLinearColor::White, 30, H-60, nullptr, 1.0f);
    }
}
