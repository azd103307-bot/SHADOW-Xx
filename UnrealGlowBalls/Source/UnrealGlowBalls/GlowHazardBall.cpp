#include "GlowHazardBall.h"
#include "Components/SphereComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Components/PointLightComponent.h"
#include "UObject/ConstructorHelpers.h"

AGlowHazardBall::AGlowHazardBall()
{
    PrimaryActorTick.bCanEverTick = false;

    PhysicsBody = CreateDefaultSubobject<USphereComponent>(TEXT("PhysicsBody"));
    SetRootComponent(PhysicsBody);
    PhysicsBody->InitSphereRadius(60.0f);
    PhysicsBody->SetCollisionProfileName(TEXT("PhysicsActor"));
    PhysicsBody->SetSimulatePhysics(true);
    PhysicsBody->SetEnableGravity(true);
    PhysicsBody->SetLinearDamping(0.12f);
    PhysicsBody->SetAngularDamping(0.22f);
    PhysicsBody->SetMassOverrideInKg(NAME_None, 24.0f, true);

    BallMesh = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("BallMesh"));
    BallMesh->SetupAttachment(PhysicsBody);
    BallMesh->SetCollisionEnabled(ECollisionEnabled::NoCollision);
    static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereAsset(TEXT("/Engine/BasicShapes/Sphere.Sphere"));
    if (SphereAsset.Succeeded())
    {
        BallMesh->SetStaticMesh(SphereAsset.Object);
        BallMesh->SetRelativeScale3D(FVector(1.2f));
    }

    BallLight = CreateDefaultSubobject<UPointLightComponent>(TEXT("BallLight"));
    BallLight->SetupAttachment(PhysicsBody);
    BallLight->SetIntensity(3600.0f);
    BallLight->SetAttenuationRadius(520.0f);
    BallLight->SetCastShadows(false);
}

void AGlowHazardBall::Launch(FLinearColor Color, const FVector& Impulse)
{
    BallLight->SetLightColor(Color);
    PhysicsBody->AddImpulse(Impulse);
}