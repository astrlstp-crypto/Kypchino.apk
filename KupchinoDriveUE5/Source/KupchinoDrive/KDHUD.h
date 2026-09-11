#pragma once

#include "CoreMinimal.h"
#include "GameFramework/HUD.h"
#include "KDHUD.generated.h"

UCLASS()
class KUPCHINODRIVE_API AKDHUD : public AHUD
{
    GENERATED_BODY()

public:
    virtual void DrawHUD() override;
};
