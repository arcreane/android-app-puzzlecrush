package com.game.puzzlecrush;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GameActivity extends AppCompatActivity {
    private ImageButton pauseBtn;
    private GameActivity activity;
    private GridLayout gridLayout;
    private RelativeLayout relativeLayout;
    private LinearLayout heroLayout;

    GameChronometer gameChronometer;
    GameTimer gameTimer;

    public static int cellWidth, screenWidth, screenHeight, gridColCount = 7, gridRowCount = 5;
    GemCell gemCellBeingDragged, gemCellBeingReplaced;
    public static int[] gems = {
            R.drawable.gem_blue,
            R.drawable.gem_green,
            R.drawable.gem_purple,
            R.drawable.gem_red,
            R.drawable.gem_yellow,
    };
    HashMap<List<Integer>, GemCell> gemCellList = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        this.activity = this;

        this.relativeLayout = (RelativeLayout) findViewById(R.id.mainAnimLayout);
        this.gridLayout = findViewById(R.id.gemGrid); // Get grid
        this.heroLayout = findViewById(R.id.heroLayout);

        // Get Screen dimensions for responsive
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        cellWidth = screenWidth / gridColCount;

        initGrid(); // Set grid col/row/size
        initBoardGems();
        initGemsSwipeListener();
        initPausePopupListeners();

        this.gameChronometer = new GameChronometer(this);
        this.gameTimer = new GameTimer(this);
    }

    private void initGrid() {
        gridLayout.setColumnCount(gridColCount);
        gridLayout.setRowCount(gridRowCount);
        gridLayout.getLayoutParams().width = screenWidth;
        gridLayout.getLayoutParams().height = cellWidth * gridRowCount;
    }

    private void initBoardGems() {
        for (int r = 0; r < gridRowCount; r++) {
            for (int c = 0; c < gridColCount; c++) {
                ImageView imageView = new ImageView(this);
                imageView.setVisibility(View.VISIBLE);
                GemCell gemCell = new GemCell(r, c, imageView);
                gridLayout.addView(gemCell.getImageView());
                gemCellList.put(Collections.unmodifiableList(Arrays.asList(r, c)), gemCell);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initGemsSwipeListener() {
        for (final GemCell gemCell : gemCellList.values()) {
            ImageView imageView = gemCell.getImageView();

            imageView.setOnTouchListener(new OnGemTouch(this) {
                @Override
                void swipeLeft() {
                    super.swipeLeft();
                    initInterchange("Left", gemCell.getX(), gemCell.getY() - 1, "translationX", -1, gemCell);
                }

                @Override
                void swipeRight() {
                    super.swipeRight();
                    initInterchange("Right", gemCell.getX(), gemCell.getY() + 1, "translationX", 1, gemCell);
                }

                @Override
                void swipeTop() {
                    super.swipeTop();
                    initInterchange("Top", gemCell.getX() - 1, gemCell.getY(), "translationY", -1, gemCell);
                }

                @Override
                void swipeBottom() {
                    super.swipeBottom();
                    initInterchange("Bottom", gemCell.getX() + 1, gemCell.getY(), "translationY", 1, gemCell);
                }

                @Override
                void clicked() {
                    super.clicked();
                    gemCell.setSelected(!gemCell.isSelected());

                    for (GemCell currGem : gemCellList.values()) {
                        boolean isSameGem = (gemCell.getX() == currGem.getX()) && (currGem.getY() == gemCell.getY());

                        if (currGem.isSelected() && !isSameGem) {
                            if (gemCell.getX() == currGem.getX() && gemCell.getY() == currGem.getY() + 1)
                                this.swipeLeft();
                            else if (gemCell.getX() == currGem.getX() && gemCell.getY() == currGem.getY() - 1)
                                this.swipeRight();
                            else if (gemCell.getY() == currGem.getY() && gemCell.getX() == currGem.getX() + 1)
                                this.swipeTop();
                            else if (gemCell.getY() == currGem.getY() && gemCell.getX() == currGem.getX() - 1)
                                this.swipeBottom();

                            currGem.setSelected(false);
                            gemCell.setSelected(false);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void initInterchange(String direction, int x, int y, String translation, int positive, GemCell gemCell) {
        Toast.makeText(GameActivity.this, direction, Toast.LENGTH_SHORT).show();

        if (gemCellList.containsKey(Arrays.asList(x, y))) {
            gemCellBeingDragged = gemCell;
            gemCellBeingReplaced = gemCellList.get(Arrays.asList(x, y));
            gemInterchange(translation, positive);
        }
    }

    private void gemInterchange(String translation, int positive) {
        final ImageView animateGemDragged = createGemToAnimate(gemCellBeingDragged);
        final ImageView animateGemReplaced = createGemToAnimate(gemCellBeingReplaced);

        ObjectAnimator animatorDragged = ObjectAnimator.ofFloat(animateGemDragged, translation, positive * cellWidth);
        animatorDragged.setDuration(300);
        ObjectAnimator animatorReplaced = ObjectAnimator.ofFloat(animateGemReplaced, translation, -positive * cellWidth);
        animatorReplaced.setDuration(300);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorDragged, animatorReplaced);
        animatorSet.start();

        gemCellBeingReplaced.getImageView().setVisibility(View.INVISIBLE);
        gemCellBeingDragged.getImageView().setVisibility(View.INVISIBLE);


        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                relativeLayout.removeView(animateGemDragged);
                relativeLayout.removeView(animateGemReplaced);

                int replacedGem = (int) gemCellBeingReplaced.getImageView().getTag();
                int draggedGem = (int) gemCellBeingDragged.getImageView().getTag();
                gemCellBeingDragged.updateImageView(replacedGem);
                gemCellBeingReplaced.updateImageView(draggedGem);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findMatches();
                    }
                }, 100);
            }
        });
    }

    private ImageView createGemToAnimate(GemCell gemCellToDuplicate) {
        final ImageView animateGem = new ImageView(this);
        animateGem.setPadding(10, 10, 10, 10);
        animateGem.setImageResource((int) gemCellToDuplicate.getImageView().getTag());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellWidth, cellWidth);
        params.leftMargin = gemCellToDuplicate.getImageView().getLeft();

        int gemGridTop = screenHeight - (gridLayout.getMeasuredHeight() + heroLayout.getMeasuredHeight());
        params.topMargin = gemGridTop + gemCellToDuplicate.getImageView().getTop();

        relativeLayout.addView(animateGem, params);
        return animateGem;
    }

    private void initPausePopupListeners() {
        this.pauseBtn = findViewById(R.id.pauseBtn); // Get pauseBtn
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Init new Popup (PausePopup Class)
                final PausePopup pausePopup = new PausePopup(activity);

                // Pause Timer/Chronometer
                gameTimer.pauseTimer();
                gameChronometer.pauseChronometer();
                // Cancel popup listener to restart timer/chronometer
                pausePopup.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        gameTimer.startTimer(); // restart timer
                        gameChronometer.startChronometer(); // restart chronometer
                    }
                });

                // On click continue Button
                pausePopup.getBtn_continue().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gameTimer.startTimer(); // restart timer
                        gameChronometer.startChronometer(); // restart chronometer
                        pausePopup.dismiss();
                    }
                });
                // On click BackMenu Button
                pausePopup.getBtn_backMenu().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent newMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(newMainActivity);
                        finish();
                        pausePopup.dismiss();
                    }
                });
                // Build/Create Popup
                pausePopup.Build();
            }
        });
    }

    private void findMatches() {
        for (GemCell gem : gemCellList.values()) {
            Object gemTag = gem.getImageView().getTag();
            if (gem.getY() > 0 && gem.getY() < gridColCount - 1) {
                GemCell leftGem = gemCellList.get(Arrays.asList(gem.getX(), gem.getY() - 1));
                GemCell rightGem = gemCellList.get(Arrays.asList(gem.getX(), gem.getY() + 1));

                if (leftGem.getImageView().getTag().equals(gemTag) && rightGem.getImageView().getTag().equals(gemTag)) {
                    leftGem.setMatched(true);
                    gem.setMatched(true);
                    rightGem.setMatched(true);
                }
            }
            if (gem.getX() > 0 && gem.getX() < gridRowCount - 1) {
                GemCell topGem = gemCellList.get(Arrays.asList(gem.getX() - 1, gem.getY()));
                GemCell bottomGem = gemCellList.get(Arrays.asList(gem.getX() + 1, gem.getY()));

                if (topGem.getImageView().getTag().equals(gemTag) && bottomGem.getImageView().getTag().equals(gemTag)) {
                    topGem.setMatched(true);
                    gem.setMatched(true);
                    bottomGem.setMatched(true);
                }
            }
        }
        removeMatches();
    }



    private void removeMatches () {
        final ArrayList<ObjectAnimator> animators = new ArrayList<>();
        final ArrayList<ImageView> animateGemsFalling = new ArrayList<>();
        final ArrayList<GemCell> gemsToDestroy = new ArrayList<>();
        final ArrayList<Integer> gemsFalling = new ArrayList<>();

        for (int r = 0; r < gridRowCount; r++) {
            for (int c = 0; c < gridColCount; c++) {
                GemCell gem = gemCellList.get(Arrays.asList(r, c));
                // Si isMatched ou empty
                if (gem.isMatched() || gem.isEmpty()) {
                    // regarder toute les row au dessus
                    for (int i = 1; i <= gridRowCount; i++) {
                        // Si la row n'existe pas (generer une nouvelle gemme)
                        if (!gemCellList.containsKey(Arrays.asList(gem.getX() + i, gem.getY()))) {
                            final ImageView animateGemFalling = new ImageView(this);
                            animateGemFalling.setPadding(10, 10, 10, 10);
                            final int randomGem = (int) Math.floor(Math.random() * gems.length);
                            animateGemFalling.setImageResource(gems[randomGem]);
                            animateGemFalling.setTag(gems[randomGem]);
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cellWidth, cellWidth);
                            params.leftMargin = gem.getImageView().getLeft();

                            int gemGridTop = screenHeight - (gridLayout.getMeasuredHeight() + heroLayout.getMeasuredHeight());
                            params.topMargin = gemGridTop + (gridRowCount + 2) * cellWidth;

                            relativeLayout.addView(animateGemFalling, params);

                            ObjectAnimator animatorFalling = ObjectAnimator.ofFloat(animateGemFalling, "translationY", -((gridRowCount + 2) - gem.getX()) * cellWidth);
                            animatorFalling.setDuration(500);

                            gem.getImageView().setVisibility(View.INVISIBLE);
//                        gem.setEmpty(false);

                            animators.add(animatorFalling);
                            animateGemsFalling.add(animateGemFalling);
                            gemsToDestroy.add(gem);
                            gemsFalling.add((int) animateGemFalling.getTag());

                            break;
                        }
                        // Si la gem n'est pas match et pas empty (remplacer par celle-ci)
                        if (!gemCellList.get(Arrays.asList(gem.getX() + i, gem.getY())).isMatched()
                                && !gemCellList.get(Arrays.asList(gem.getX() + i, gem.getY())).isEmpty()) {
                            GemCell gemFalling = gemCellList.get(Arrays.asList(gem.getX() + i, gem.getY()));

                            gemFalling.setEmpty(true);

                            ImageView animateGemFalling = createGemToAnimate(gemFalling);
                            ObjectAnimator animatorFalling = ObjectAnimator.ofFloat(animateGemFalling, "translationY", -(gemFalling.getX() - gem.getX()) * cellWidth);
                            animatorFalling.setDuration(500);
                            gemFalling.getImageView().setVisibility(View.INVISIBLE);
                            gem.getImageView().setVisibility(View.INVISIBLE);
//                        gem.setEmpty(false);

                            animators.add(animatorFalling);
                            animateGemsFalling.add(animateGemFalling);
                            gemsToDestroy.add(gem);
                            gemsFalling.add((int) gemFalling.getImageView().getTag());

                            break;
                        }
                    }
                }
            }
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators.toArray(new ObjectAnimator[1]));
        animatorSet.start();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                for (int i = 0; i < gemsToDestroy.size(); i++) {
                    relativeLayout.removeView(animateGemsFalling.get(i));
                    gemsToDestroy.get(i).setMatched(false);
                    gemsToDestroy.get(i).setEmpty(false);
//                    gemsToDestroy.get(i).getImageView().setBackgroundResource(0);
                    gemsToDestroy.get(i).updateImageView(gemsFalling.get(i));
                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findMatches();
                    }
                }, 500);
            }
        });
    }
}
