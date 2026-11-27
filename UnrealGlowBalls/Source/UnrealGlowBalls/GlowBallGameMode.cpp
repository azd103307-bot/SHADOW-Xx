#include "GlowBallGameMode.h"
#include "GlowBallPawn.h"
#include "GlowOrbPickup.h"
#include "GlowHazardBall.h"
#include "Engine/StaticMeshActor.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/DirectionalLight.h"
#include "Components/DirectionalLightComponent.h"
#include "Engine/PointLight.h"
#include "Components/PointLightComponent.h"
#include "Engine/StaticMesh.h"
#include "Engine/Engine.h"

AGlowBallGameMode::AGlowBallGameMode()
{
    DefaultPawnClass = AGlowBallPawn::StaticClass();
}

void AGlowBallGameMode::BeginPlay()
{
    Super::BeginPlay();

    UWorld* World = GetWorld();
    if (!World) return;

    UStaticMesh* CubeAsset = LoadObject<UStaticMesh>(nullptr, TEXT("/Engine/BasicShapes/Cube.Cube"));
    UStaticMesh* SphereAsset = LoadObject<UStaticMesh>(nullptr, TEXT("/Engine/BasicShapes/Sphere.Sphere"));

    auto SpawnBlock = [World, CubeAsset](const FVector& Location, const FVector& Scale)
    {
        if (!CubeAsset) return;
        AStaticMeshActor* Block = World->SpawnActor<AStaticMeshActor>(Location, FRotator::ZeroRotator);
        if (!Block) return;
        Block->GetStaticMeshComponent()->SetStaticMesh(CubeAsset);
        Block->SetActorScale3D(Scale);
        Block->GetStaticMeshComponent()->SetMobility(EComponentMobility::Static);
        Block->GetStaticMeshComponent()->SetCollisionProfileName(TEXT("BlockAll"));
    };

    // Broad arena floor and perimeter walls.
    SpawnBlock(FVector(0, 0, -122), FVector(50, 50, 1));
    SpawnBlock(FVector(0, 2520, 120), FVector(50, 1, 2.4f));
    SpawnBlock(FVector(0, -2520, 120), FVector(50, 1, 2.4f));
    SpawnBlock(FVector(2520, 0, 120), FVector(1, 50, 2.4f));
    SpawnBlock(FVector(-2520, 0, 120), FVector(1, 50, 2.4f));

    // Pillars and low barriers create a simple rolling obstacle course.
    for (int32 i = 0; i < 8; ++i)
    {
        const float X = -1500.0f + (i % 4) * 1000.0f;
        const float Y = (i < 4) ? -900.0f : 900.0f;
        SpawnBlock(FVector(X, Y, 150), FVector(1.0f, 1.0f, 3.0f));
    }
    SpawnBlock(FVector(0, 0, 35), FVector(8.0f, 0.45f, 0.35f));
    SpawnBlock(FVector(1050, 0, 35), FVector(0.45f, 8.0f, 0.35f));
    SpawnBlock(FVector(-1050, 0, 35), FVector(0.45f, 8.0f, 0.35f));

    // Floating collectible energy orbs.
    const FVector OrbPositions[12] = {
        FVector(-1850,-1650,145), FVector(-900,-1650,155), FVector(0,-1650,145),
        FVector(900,-1650,155), FVector(1850,-1650,145), FVector(1850,-500,160),
        FVector(1850,650,145), FVector(900,1650,155), FVector(0,1650,145),
        FVector(-900,1650,155), FVector(-1850,1650,145), FVector(-1850,450,160)
    };
    for (const FVector& Position : OrbPositions)
    {
        World->SpawnActor<AGlowOrbPickup>(Position, FRotator::ZeroRotator);
    }

    // Roaming physics balls add moving collisions and color to the arena.
    const FVector HazardStarts[6] = {
        FVector(-1750, -900, 260), FVector(1750, -900, 260),
        FVector(-1750, 900, 260), FVector(1750, 900, 260),
        FVector(0, -2050, 260), FVector(0, 2050, 260)
    };
    const FLinearColor HazardColors[6] = {
        FLinearColor(0.05f, 0.55f, 1.0f), FLinearColor(1.0f, 0.12f, 0.25f),
        FLinearColor(0.35f, 1.0f, 0.12f), FLinearColor(0.85f, 0.15f, 1.0f),
        FLinearColor(1.0f, 0.55f, 0.05f), FLinearColor(0.05f, 1.0f, 0.8f)
    };
    for (int32 i = 0; i < 6; ++i)
    {
        AGlowHazardBall* Hazard = World->SpawnActor<AGlowHazardBall>(
            HazardStarts[i], FRotator::ZeroRotator);
        if (Hazard)
        {
            const FVector TowardCenter = (-HazardStarts[i]).GetSafeNormal();
            Hazard->Launch(HazardColors[i], TowardCenter * 9000.0f + FVector(0, 0, 900.0f));
        }
    }

    ADirectionalLight* Sun = World->SpawnActor<ADirectionalLight>(
        FVector(0, 0, 900), FRotator(-48, -32, 0));
    if (Sun)
    {
        Sun->GetLightComponent()->SetIntensity(8.0f);
        Sun->GetLightComponent()->SetLightColor(FLinearColor(0.62f, 0.78f, 1.0f));
    }

    if (SphereAsset)
    {
        for (int32 i = 0; i < 4; ++i)
        {
            AStaticMeshActor* Beacon = World->SpawnActor<AStaticMeshActor>(
                FVector((i - 1.5f) * 1050.0f, 0.0f, 20.0f), FRotator::ZeroRotator);
            if (!Beacon) continue;
            Beacon->GetStaticMeshComponent()->SetStaticMesh(SphereAsset);
            Beacon->SetActorScale3D(FVector(0.7f));
            Beacon->GetStaticMeshComponent()->SetCollisionEnabled(ECollisionEnabled::NoCollision);
            UPointLightComponent* Lamp = NewObject<UPointLightComponent>(Beacon, TEXT("ArenaBeaconLight"));
            if (Lamp)
            {
                Lamp->SetupAttachment(Beacon->GetRootComponent());
                Lamp->SetLightColor(i % 2 == 0 ? FLinearColor(0.0f, 0.55f, 1.0f) : FLinearColor(0.7f, 0.1f, 1.0f));
                Lamp->SetIntensity(4200.0f);
                Lamp->SetAttenuationRadius(700.0f);
                Lamp->RegisterComponent();
            }
        }
    }
}

void AGlowBallGameMode::RegisterOrbCollected()
{
    ++CollectedOrbs;
    if (GEngine)
    {
        const FString Message = FString::Printf(TEXT("Energy orbs: %d / %d"), CollectedOrbs, TotalOrbs);
        GEngine->AddOnScreenDebugMessage(4102, 2.5f, FColor(70, 230, 255), Message);
    }
    if (CollectedOrbs >= TotalOrbs && GEngine)
    {
        GEngine->AddOnScreenDebugMessage(4103, 8.0f, FColor::Green, TEXT("ARENA CLEARED!"));
    }
}