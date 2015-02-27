/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.ai.tests.steer.scene2d.formation;

import com.badlogic.gdx.ai.fma.Formation;
import com.badlogic.gdx.ai.fma.FormationMember;
import com.badlogic.gdx.ai.fma.FreeSlotAssignmentStrategy;
import com.badlogic.gdx.ai.fma.SlotAssignment;
import com.badlogic.gdx.ai.fma.SoftRoleSlotAssignmentStrategy;
import com.badlogic.gdx.ai.fma.SoftRoleSlotAssignmentStrategy.SlotCostProvider;
import com.badlogic.gdx.ai.fma.patterns.DefensiveCircleFormationPattern;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.ai.steer.behaviors.BlendedSteering;
import com.badlogic.gdx.ai.steer.behaviors.LookWhereYouAreGoing;
import com.badlogic.gdx.ai.steer.behaviors.ReachOrientation;
import com.badlogic.gdx.ai.steer.behaviors.Seek;
import com.badlogic.gdx.ai.steer.behaviors.Wander;
import com.badlogic.gdx.ai.steer.limiters.AngularLimiter;
import com.badlogic.gdx.ai.steer.limiters.LinearAccelerationLimiter;
import com.badlogic.gdx.ai.steer.limiters.LinearLimiter;
import com.badlogic.gdx.ai.steer.limiters.NullLimiter;
import com.badlogic.gdx.ai.tests.SteeringBehaviorsTest;
import com.badlogic.gdx.ai.tests.steer.scene2d.Scene2dLocation;
import com.badlogic.gdx.ai.tests.steer.scene2d.Scene2dSteeringTest;
import com.badlogic.gdx.ai.tests.steer.scene2d.SteeringActor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** A class to test and experiment with the {@link Seek} behavior.
 * 
 * @autor davebaol */
public class Scene2dFormationTest extends Scene2dSteeringTest {

	SteeringActor character;

	Formation<Vector2> formation;

	final boolean bounded;

	public Scene2dFormationTest (SteeringBehaviorsTest container, boolean bounded) {
		super(container, "Formation (" + (bounded ? "Bounded)" : "Free)"));
		this.bounded = bounded;
	}

	@Override
	public void create (final Table table) {

		character = new SteeringActor(container.target, false);
		character.setMaxLinearSpeed(50);

		// Click the character to add a new member
		character.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				int badlogics = 0;
				int fishes = 0;
				for (int i = 0; i < formation.getSlotCount(); i++) {
					SlotAssignment<Vector2> slot = formation.getSlot(i);
					SteeringActorFormationMember safm = (SteeringActorFormationMember)slot.member;
					if (safm.getRegion() == container.badlogicSmall)
						badlogics++;
					else
						fishes++;
				}
				SteeringActorFormationMember safm = createFormationMember(fishes < badlogics ? 0 : 1, table);
				formation.addMember(safm);
				table.addActor(safm);
			}
		});

		Wander<Vector2> wanderSB = new Wander<Vector2>(character) //
			.setLimiter(new LinearAccelerationLimiter(40)) //
			.setFaceEnabled(false) // set to 0 because independent facing is off
			.setAlignTolerance(0.001f) //
			.setDecelerationRadius(5) //
			.setTimeToTarget(0.1f) //
			.setWanderOffset(70) //
			.setWanderOrientation(10) //
			.setWanderRadius(40) //
			.setWanderRate(MathUtils.PI / 5);
		character.setSteeringBehavior(wanderSB);

		table.addActor(character);

		DefensiveCircleFormationPattern<Vector2> defensiveCirclePattern = new DefensiveCircleFormationPattern<Vector2>(
			container.greenFish.getRegionWidth());
		
		formation = new Formation<Vector2>(character, defensiveCirclePattern, null);

		// Set the slot assignment strategy
		if (bounded) {
			SlotCostProvider<Vector2> slotCostProvider = new SlotCostProvider<Vector2>() {

				@Override
				public float getCost (FormationMember<Vector2> member, int slotNumber) {
					SteeringActorFormationMember safm = (SteeringActorFormationMember)member;
					boolean isCorrectSlot = slotNumber % 2 == 0;
					if (safm.getRegion() != container.greenFish) isCorrectSlot = !isCorrectSlot;
// float cost = isCorrectSlot ? 0f : 5000000000f;
					float cost = isCorrectSlot ? 0f : 10000f * 2 * formation.getSlotCount();
					Scene2dLocation slotTarget = ((SteeringActorFormationMember)formation.getSlot(slotNumber).member).target;
					return cost + safm.getPosition().dst(slotTarget.getPosition());
				}
			};
			formation.setSlotAssignmentStrategy(new SoftRoleSlotAssignmentStrategy<Vector2>(slotCostProvider));
		} else
			formation.setSlotAssignmentStrategy(new FreeSlotAssignmentStrategy<Vector2>());

		for (int i = 0; i < 4; i++) {
			SteeringActorFormationMember safm = createFormationMember(i, table);

			formation.addMember(safm);
			table.addActor(safm);

			// debug
			for (int k = 0; k < formation.getSlotCount(); k++) {
				System.out.println("slot " + formation.getSlot(k).slotNumber + ": "
					+ (formation.getSlot(k).slotNumber % 2 == 0 ? "fish" : "badlogic"));
			}

		}

		character.setPosition(container.stageWidth / 2, container.stageHeight / 2, Align.center);

// Table detailTable = new Table(container.skin);
//
// detailTable.row();
// final Label labelMaxLinAcc = new Label("Max.linear.acc.[" + seekSB.getMaxLinearAcceleration() + "]", container.skin);
// detailTable.add(labelMaxLinAcc);
// detailTable.row();
// Slider maxLinAcc = new Slider(0, 10000, 20, false, container.skin);
// maxLinAcc.setValue(seekSB.getMaxLinearAcceleration());
// maxLinAcc.addListener(new ChangeListener() {
// @Override
// public void changed (ChangeEvent event, Actor actor) {
// Slider slider = (Slider)actor;
// seekSB.setMaxLinearAcceleration(slider.getValue());
// labelMaxLinAcc.setText("Max.linear.acc.[" + slider.getValue() + "]");
// }
// });
// detailTable.add(maxLinAcc);
//
// detailTable.row();
// addSeparator(detailTable);
//
// detailTable.row();
// addMaxSpeedController(detailTable, character);
//
// detailWindow = createDetailWindow(detailTable);
	}

	@Override
	public void render () {
		formation.updateSlots();
	}

	@Override
	public void dispose () {
	}

	private SteeringActorFormationMember createFormationMember (int type, final Table table) {

		SteeringActorFormationMember safm = new SteeringActorFormationMember(type % 2 == 0 ? container.greenFish
			: container.badlogicSmall, true) {

			ReachOrientation<Vector2> reachOrientationSB;
			LookWhereYouAreGoing<Vector2> lookWhereYouAreGoingSB;

			@Override
			public void act (float delta) {
				if (reachOrientationSB == null) {

					Arrive<Vector2> arriveSB = new Arrive<Vector2>(this, this.getTargetLocation()) //
						.setLimiter(new LinearLimiter(3500, 1000)) //
						.setTimeToTarget(0.1f) //
						.setArrivalTolerance(0.001f) //
						.setDecelerationRadius(40);

					this.reachOrientationSB = new ReachOrientation<Vector2>(this, this.getTargetLocation()) //
						.setLimiter(new AngularLimiter(100, 20)) //
						.setTimeToTarget(0.1f) //
						.setAlignTolerance(0.001f) //
						.setDecelerationRadius(MathUtils.PI);

					this.lookWhereYouAreGoingSB = new LookWhereYouAreGoing<Vector2>(this) //
						.setLimiter(new AngularLimiter(100, 20)) //
						.setTimeToTarget(0.1f) //
						.setAlignTolerance(0.001f) //
						.setDecelerationRadius(MathUtils.PI);

					BlendedSteering<Vector2> reachPositionAndOrientationSB = new BlendedSteering<Vector2>(this)
						.setLimiter(NullLimiter.NEUTRAL_LIMITER) //
						.add(arriveSB, 1f) //
						.add(reachOrientationSB, 1f) //
						.add(lookWhereYouAreGoingSB, 1f);

					this.setSteeringBehavior(reachPositionAndOrientationSB);
				}

				boolean lwyag = getPosition().dst2(getTargetLocation().getPosition()) > 1000;
				lookWhereYouAreGoingSB.setEnabled(lwyag);
				reachOrientationSB.setEnabled(!lwyag);
				super.act(delta);
			}
		};

		// Click this member to remove it
		safm.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				SteeringActorFormationMember safm = (SteeringActorFormationMember)event.getListenerActor();
				formation.removeMember(safm);
				table.removeActor(safm);
			}
		});

		return safm;
	}
}
