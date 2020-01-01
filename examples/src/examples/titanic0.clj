(ns examples.titanic0
  (:require [notespace.v0.note :as note :refer [note note-md note-as-md note-hiccup note-as-hiccup note-void]]
            [clojuress.v0.note :refer [note-r]]))

(note-md "
#Clouress example: Titanic #0 - naive function wrappers

This notebook is a variation of Pradeep Tripathi's Titanic [Kaggle solution](https://www.kaggle.com/pradeeptripathi/prediction-of-titanic-survival-using-r/code) in R. Instead of writing it in R as the original, we write it in Clojure, and call R from Clojure.

The goal is to study the Clojure-R interop, and expecially experiment with various ways to define Clojure functions corresponding to R functions.

In this first, naive version, the corresponding Clojure functions are rather simple. They expect a varying number of arguments, and pass those arguments to R function calls by a rather generic way (as defined by Clojuresss)

We do not try to replace the rather imperative style of the original tutorial. Rather, we try to write something that is as close as possible to the original.

We have leanred a lot from this use case. It did expose lots of issues and open questions about the Clojuress API and implementation. Note, however, that the piece of R code that we are mimicing here is not so typical to the current tidyverse trends -- there is no heavy use of dplyr, tidy evaluation, etc. It may be a good idea to study other examples that have more of those.

This notebook has been written in [notespace](https://github.com/scicloj/notespace) -- an experimental Clojure library that allows one to use regular Clojure namespaces as notebooks, thus enabling interactive literate programming from ones' favourite Clojure editor and REPL.

daslu, Jan. 2020")

(note-hiccup
 [:hr])

(note-md "## Bringing the neecessary R functions
Here are most of the functions that we need, brought by the standard `require-r` mechanism, inspired by [libpython-clj](https://github.com/cnuernber/libpython-clj])'s `require-python` (though not as sophisticated at the moment). In function names, dots are changed to hyphens.")

(note-void
 (require
  '[clojuress.v0.r :as r
    :refer [r r->clj
            na empty-symbol
            r== r!= r< r> r<= r>= r& r&& r| r||
            r-str print-summary print-table
            r+
            bra bra<- brabra brabra<- colon]]
  '[clojuress.v0.applications.plotting :refer [plotting-function->svg
                                               ggplot->svg]]
  '[clojuress.v0.require :refer [require-r]]
  '[clojure.java.shell :refer [sh]]
  '[clojure.string :as string]))

(note-void
 (r/discard-all-sessions))

(note-void
 (require-r
  '[base :refer [round names ! set.seed sum which rnorm lapply sapply %in% table list.files c paste colnames row.names cbind gsub <- $ $<- as.data.frame data.frame nlevels factor expression is.na strsplit as.character]]
  '[stats :refer [median predict]]
  '[ggplot2 :refer [ggsave qplot ggplot aes facet_grid geom_density geom_text geom_histogram geom_bar scale_x_continuous scale_y_continuous labs coord_flip geom_vline geom_hline geom_boxplot]]
  '[ggthemes :refer [theme_few]]
  '[scales :refer [dollar_format]]
  '[graphics :refer [par plot hist dev.off legend]]
  '[dplyr :refer [mutate bind_rows summarise group_by]]
  '[utils :refer [read.csv write.csv head]]
  '[mice :refer [mice complete]]
  '[randomForest :refer [randomForest importance]]))


(note-md
 "
## Introduction -- Prediction of Titanic Survival using Random Forest

Pradeep Tripathi's solution will use randomForest to create a
model predicting survival on the Titanic.
")

(note-md
 "
## Reading test and train data
This step assumes that the [Titanic data](https://www.kaggle.com/c/titanic/data) lies under the `resources/data/` path under your Clojure project.
")

(note-void
 (def data-path
   (-> "pwd"
       sh
       :out
       string/trim
       (str "/resources/data/"))))

(note-md
 "
```
# Original code:
list.files('../input')
```
")

(note-r
 (list-files data-path))

(note-md
 "

```
# Original code:
train <-read.csv('../input/train.csv', stringsAsFactors = F)
test  <-read.csv('../input/test.csv', stringsAsFactors = F)
```
")

(note-void
 (def train (read-csv
             (str data-path "train.csv.gz")
             :stringsAsFactors false))
 (def test (read-csv
            (str data-path "test.csv.gz")
            :stringsAsFactors false)))


(note-md
 "## Combining test and train data
As explained by Thripathi, the Random Forest algorithm will use the Bagging method to create multiple random samples with replacement from the dataset, that will be treated as training data, while the out of bag samples will be treated as test data.
")

(note-md
 "
```
# Original code:
titanic<-bind_rows(train,test)
```
")

(note-void
 (def titanic
   (bind_rows train test)))

(note-md
 "## Data check")

(note-md "
```
# Original code:
str(titanic)
summary(titanic)
head(titanic)
```
")

(note-r (r-str titanic))
(note-r (print-summary titanic))
(note-r (head titanic))

(note-md "Tripathi:
We've got a sense of our variables, their class type,
3 and the first few observations of each. We know we're working with 
1309 observations of 12 variables. ")

(note-md "## Feature engineering
Thripathi's explanation:
We can break down Passenger name into additional meaningful variables
which can feed predictions or be used in the creation of additional new
variables. For instance, passenger title is contained within the passenger
name variable and we can use surname to represent families.
")

(note-md "
```
# Original code:
colnames(titanic)
```")

(note-r
 (colnames titanic))

(note-md "
Retrieve title from passenger names
```
# Original code:
titanic$title<-gsub('(.*, )|(\\..*)', '', titanic$Name)
```")

(note-void
 (def titanic
   ($<- titanic 'title
     (gsub "(.*, )|(\\..*)"
           ""
           ($ titanic 'Name)))))


(note-md
 "Show title counts by sex
```
# Original code:
table(titanic$Sex, titanic$title)
```
")

(note-md "Clojuress can covert an R frequency table to a Clojure data structure:")

(note-r
 (table ($ titanic 'Sex)
        ($ titanic 'title)))

(note-md "Sometimes, it is convenient to first convert it to an R data frame:")

(note-r
 (as-data-frame
  (table ($ titanic 'Sex)
         ($ titanic 'title))))

(note-md "Sometimes, it is convenient to use the way R prints a frequency table.")

(note-as-md
 (->> (print-table ($ titanic 'Sex)
                   ($ titanic 'title))
      r-lines->md))

(note-md "Convert titles with low count into a new title, and rename/reassign Mlle, Ms and Mme.
```
# Original code:
unusual_title<-c('Dona', 'Lady', 'the Countess','Capt', 'Col', 'Don', 
                 'Dr', 'Major', 'Rev', 'Sir', 'Jonkheer')
```
")

(note-void
 (def unusual-title
   ["Dona", "Lady", "the Countess","Capt", "Col", "Don",
    "Dr", "Major", "Rev", "Sir", "Jonkheer"]))

(note-md "
```
# Original code:
titanic$title[titanic$title=='Mlle']<-'Miss'
titanic$title[titanic$title=='Ms']<-'Miss'
titanic$title[titanic$title=='Mme']<-'Mrs'
titanic$title[titanic$title %in% unusual_title]<-'Unusual Title'
```")

(note-void
 (def titanic
   (-> titanic
       (bra<- (r== ($ titanic 'title) "Mlle")
              "title"
              "Miss")
       (bra<- (r== ($ titanic 'title) "Ms")
              "title"
              "Miss")
       (bra<- (r== ($ titanic 'title) "Mme")
              "title"
              "Mrs")
       (bra<- (%in% ($ titanic 'title) unusual-title)
              "title"
              "Mrs"))))

(note-md "Check the title count again:
```
# Original code:
table(titanic$Sex, titanic$title)
```")

(note-as-md
 "trying again:"
 (->> (print-table ($ titanic 'Sex)
                   ($ titanic 'title))
      r-lines->md))


(note-md
 "Create a variable which contain the surnames of passengers.
```
# Original code:
titanic$surname<-sapply(titanic$Name, function(x) strsplit(x,split='[,.]')[[1]][1])
nlevels(factor(titanic$surname)) ## 875 unique surnames
```
")

(note-void
 (def titanic
   ($<- titanic 'surname
        (sapply ($ titanic 'Name)
                (r "function(x) strsplit(x,split='[,.]')[[1]][1]")))))

(note-r
 (-> titanic
     ($ 'surname)
     factor
     nlevels))

(note-md "Tripathi:
Family size variable: We are going to create a variable \"famsize\" to know
the number of family members. It includes number of sibling/number of parents
and children+ passenger themselves

```
# Original code:
titanic$famsize <- titanic$SibSp + titanic$Parch + 1
```
")

(note-void
 (def titanic
   ($<- titanic 'famsize
        (r+ ($ titanic 'SibSp)
            ($ titanic 'Parch)
            1))))

(note-md
 "Create a family variable:
```
# Original code:
titanic$family <- paste(titanic$surname, titanic$famsize, sep='_')
```")



(note-void
 (def titanic
   ($<- titanic 'family
        (paste ($ titanic 'surname)
               ($ titanic 'famsize)
               :sep "_"))))

(note-md
 "Visualize the relationship between family size & survival:
```
 ggplot(titanic[1:891,], aes(x = famsize, fill = factor(Survived))) +
   geom_bar(stat='count', position='dodge') +
   scale_x_continuous(breaks=c(1:11)) +
   labs(x = 'Family Size') +
   theme_few()
```")

(note-as-hiccup
 (-> titanic
     (bra (colon 1 891)
          (empty-symbol))
     (ggplot (aes :x 'famsize
                  :fill '(factor Survived)))
     (r+ (geom_bar :stat "count"
                   :position "dodge")
         (scale_x_continuous :breaks (colon 1 11))
         (labs :x "Family Size")
         (theme_few))
     ggplot->svg))

(note-md "Tripathi:
Explanation: We can see that there's a survival penalty to single/alone, and
those with family sizes above 4. We can collapse this variable into three
levels which will be helpful since there are comparatively fewer large families.

Discretize family size:
```
# Original code:
titanic$fsizeD[titanic$famsize == 1] <- 'single'
titanic$fsizeD[titanic$famsize < 5 & titanic$famsize> 1] <- 'small'
titanic$fsizeD[titanic$famsize> 4] <- 'large'
```")

(note-void
 (def titanic
   (-> titanic
       (bra<- (r== ($ titanic 'famsize) 1)
              "fsizeD"
              "single")
       (bra<- (r& (r< ($ titanic 'famsize) 5)
                     (r> ($ titanic 'famsize) 1))
              "fsizeD"
              "small")
       (bra<- (r> ($ titanic 'famsize) 4)
              "fsizeD" "large"))))

(note-md "Let us check if it makes sense:")

(note-r
 (-> titanic
     ($ 'fsizeD)
     table))

(note-md "And let us make sure there are no missing values:")

(note-r
 (-> titanic
     ($ 'fsizeD)
     is-na
     table))

(note-md
 "Tripathi: There's could be some useful information in the passenger cabin variable
including about their deck, so Retrieve deck from Cabin variable.

```
# Original code:
titanic$Cabin[1:28]
```")

(note-r
 (-> titanic
     (bra (colon 1 28)
          "Cabin")))

(note-md
 "
The first character is the deck:
```
# Original code:
strsplit(titanic$Cabin[2], NULL) [[1]]
```")

(note-r
 (-> titanic
     ($ 'Cabin)
     (bra 2)
     (strsplit nil)
     (brabra 1)))

(note-md "Deck variable:
```
# Original R code:
titanic$deck<-factor(sapply(titanic$Cabin, function(x) strsplit(x, NULL)[[1]][1]))
```")

(note-void
 (def titanic
   ($<- titanic 'deck
        (factor (sapply ($ titanic 'Cabin)
                        (r "function(x) strsplit(x, NULL)[[1]][1]"))))))

(note-md "Let us check:")

(note-r
 (-> titanic
     ($ 'deck)
     table))

(note-md "## Missing values")

(note-r "updated summary" (print-summary titanic))

(note-md "Thripathi's explanation, following the summary:
- Age : 263 missing values
- Fare : 1 missing values
- Embarked : 2 missing values
- survived:too many
- Cabin : too many")

(note-md "Missing value in Embarkment -- Tripathi:
 Now we will explore missing values and rectify it
 through imputation. There are a number of different ways we could go about
doing this. Given the small size of the dataset, we probably should not opt
 for deleting either entire observations (rows) or variables (columns)
 containing missing values. We're left with the option of replacing missing
 values with sensible values given the distribution of the data, e.g., the
 mean, median or mode.

To know which passengers have no listed embarkment port:
```
# Original code:
titanic$Embarked[titanic$Embarked == \"\"] <- NA
titanic[(which(is.na(titanic$Embarked))), 1]
```")

(note-md "Marking as missing:")

(note-void
 (def titanic
   (bra<- titanic
          (r== ($ titanic 'Embarked) "")
          "Embarked"
          (r/na))))

(note-md "Checking which has missing port:")

(note-r
 (-> titanic
     (bra (-> titanic
              ($ 'Embarked)
              is-na
              which)
          1)))

(note-md
 "Tripathi: Passengers 62 and 830 are missing Embarkment.

```
# Original code:
titanic[c(62, 830), 'Embarked']
```")

(note-r
 (-> titanic
     (bra [62 830]
          "Embarked")))

(note-md "Tripathi:
So Passenger numbers 62 and 830 are each missing their embarkment ports.
Let's look at their class of ticket and their fare.

```
# Original code:
titanic[c(62, 830), c(1,3,10)]
```")

(note-r
 (-> titanic
     (bra [62 830]
          [1 3 10])))

(note-md "Alternatively:")

(note-r
 (-> titanic
     (bra [62 830]
          ["PassengerId" "Pclass" "Fare"])))

(note-md "Thripathi's explanation:
Both passengers had first class tickets that they spent 80 (pounds?) on. 
Let's see the embarkment ports of others who bought similar kinds of tickets.

First way of handling missing value in Embarked:


```
# Original code:
titanic%>%
  group_by(Embarked, Pclass) %>%
  filter(Pclass == \"1\") %>%
  filter(Pclass == \"1\") %>%
  filter(Pclass == \"1\") %>%
  summarise(mfare = median(Fare),n = n())
```")

(note-r
 (-> titanic
     (group_by 'Embarked 'Pclass)
     (r-filter '(== Pclass "1"))
     (summarise :mfare '(median Fare)
                :n '(n))))

(note-md "Tripathi:
Looks like the median price for a first class ticket departing from 'C'
(Charbourg) was 77 (in comparison to our 80). While first class tickets
departing from 'Q' were only slightly more expensiive (median price 90),
only 3 first class passengers departed from that port. It seems far
more likely that passengers 62 and 830 departed with the other 141
first-class passengers from Charbourg.

Second Way of handling missing value in Embarked:


```
# Original code:
embark_fare <- titanic %>%
  filter(PassengerId != 62 & PassengerId != 830)
embark_fare
```")

(note-r
 (def embark_fare
   (-> titanic
       (r-filter '(& (!= PassengerId 62)
                     (!= PassengerId 830))))))


(note-md "Use ggplot2 to visualize embarkment, passenger class, & median fare:


```
# Original code:
ggplot(embark_fare, aes(x = Embarked, y = Fare, fill = factor(Pclass))) +
geom_boxplot() +
geom_hline(aes(yintercept=80), 
              colour='red', linetype='dashed', lwd=2) +
scale_y_continuous(labels=dollar_format()) +
theme_few()
```")

(note-as-hiccup
 (-> embark_fare
     (ggplot (aes :x 'Embarked
                  :y 'Fare
                  :fill '(factor Pclass)))
     (r+ (geom_boxplot)
         (geom_hline (aes :yintercept 80)
                     :colour "red"
                     :linetype "dashed"
                     :lwd 2)
         (scale_y_continuous :labels (dollar_format)))
     ggplot->svg))

(note-md
 "Tripathi:
From plot we can see that The median fare for a first class passenger
departing from Charbourg ('C') coincides nicely with the $80 paid by our
embarkment-deficient passengers. I think we can safely replace the NA values
with 'C'.
Since their fare was $80 for 1st class, they most likely embarked from 'C'.


```
# Original code:
titanic$Embarked[c(62, 830)] <- 'C'
```")

(note-void
 (def titanic
   (bra<- titanic [62 830] "Embarked"
          "C")))

(note-md "A missing value in fare.
Thripathi's explanation:
To know Which passenger has no fare information:

```
# Original code:
titanic[(which(is.na(titanic$Fare))) , 1] 
```
")

(note-r
 (-> titanic
     (bra (-> titanic
              ($ 'Fare)
              is-na
              which)
          1)))

(note-md "Tripathi:
 Looks like Passenger number 1044 has no listed Fare

 Where did this passenger leave from? What was their class?


```
# Original code:
 titanic[1044, c(3, 12)]
```")

(note-r
 (-> titanic
     (bra 1044 [3 12])))

(note-md "Tripathi:
Another way to know about passenger id 1044 :Show row 1044
```
# Original code:
titanic[1044, ]
```")

(note-r
 (-> titanic
     (bra 1044 (empty-symbol))))

(note-md "Thripathi's explanation:
Looks like he left from 'S' (Southampton) as a 3rd class passenger.
Let's see what other people of the same class and embarkment port paid for
their tickets.

# First way:
titanic%>%
  filter(Pclass == '3' & Embarked == 'S') %>%
  summarise(missing_fare = median(Fare, na.rm = TRUE))
")

(note-r
 (-> titanic
     (r-filter '(& (== Pclass "3")
                   (== Embarked "S")))
     (summarise :missing_fare '(median Fare :na.rm true))))

(note-md "Tripathi:
Looks like the median cost for a 3rd class passenger leaving out of
Southampton was 8.05. That seems like a logical value for this passenger
to have paid.

Second way:


```
# Original code:
ggplot(titanic[titanic$Pclass == '3' & titanic$Embarked == 'S', ],
       aes(x = Fare)) +
  geom_density(fill = '#99d6ff', alpha=0.4) +
  geom_vline(aes(xintercept=median(Fare, na.rm=T)),
             colour='red', linetype='dashed', lwd=1) +
  scale_x_continuous(labels=dollar_format()) +
  theme_few()
```")

(note-as-hiccup
 (-> titanic
     (bra (r& (r== ($ titanic 'Pclass) 3)
                 (r== ($ titanic 'Embarked) "S"))
          (empty-symbol))
     (ggplot (aes :x 'Fare))
     (r+ (geom_density :fill "#99d6ff"
                       :alpha 0.4)
         (geom_vline (aes :xintercept
                           '(median Fare :na.rm true))
                     :colour "red"
                     :linetype "dashed"
                     :lwd 1)
         (scale_x_continuous :labels (dollar_format)))
     ggplot->svg))

(note-md "Tripathi:
From this visualization, it seems quite reasonable to replace the NA Fare 
value with median for their class and embarkment which is $8.05.

Replace that NA with 8.05

```
# Original code:
titanic$Fare[1044] <- 8.05
summary(titanic$Fare)
```")

(note-r
 (def titanic
   (bra<- titanic 1044 "Fare"
          8.05))
 (-> titanic
     ($ 'Fare)
     print-summary))

(note-md "Tripathi:
Another way of Replace missing fare value with median fare for class/embarkment:

```
# Original code:
titanic$Fare[1044] <- median(titanic[titanic$Pclass == '3' & titanic$Embarked == 'S', ]$Fare, na.rm = TRUE)
```")

(note-void
 (def titanic
   (bra<- titanic 1044 "Fare"
          (-> titanic
              (bra (r& (r== ($ titanic 'Pclass) 3)
                          (r== ($ titanic 'Embarked) "S"))
                   "Fare")
              (median :na.rm true)))))


(note-md "Missing Value in Age.

Tripathi: Show number of missing Age values.

```
# Original code:
sum(is.na(titanic$Age)) ```")


(note-r
 "before"
 (-> titanic
     ($ 'Age)
     is-na
     sum))

(note-md "Tripathi:
263 passengers have no age listed. Taking a median age of all passengers
doesn't seem like the best way to solve this problem, so it may be easiest to
try to predict the passengers' age based on other known information.

To predict missing ages, I'm going to use the mice package. To start with
I will factorize the factor variables and then perform
mice(multiple imputation using chained equations).

Set a random seed:

```
# Original code:
set.seed(129)
```")

(note-void
 (set-seed 129))

(note-md
"Tripathi: Perform mice imputation, excluding certain less-than-useful variables:

```
# Original code:
mice_mod <- mice(titanic[, !names(titanic) %in% c('PassengerId','Name','Ticket','Cabin','Family','Surname','Survived')], method='rf') 
```")

(note-void
 (def mice-mod
   (-> titanic
       (bra (empty-symbol)
            (-> titanic
                names
                (%in% ["PassengerId","Name","Ticket","Cabin","Family","Surname","Survived"])
                !))
       (mice :method "rf"))))


(note-md "Save the complete output.
```
# Original code:
mice_output <- complete(mice_mod)
```")

(note-void
 (def mice-output
   (complete mice-mod)))

(note-md "Tripathi:
Let's compare the results we get with the original distribution of
passenger ages to ensure that nothing has gone completely awry.

Plot age distributions:

```
# Original code:
par(mfrow=c(1,2))
hist(titanic$Age, freq=F, main='Age: Original Data',
     col='darkred', ylim=c(0,0.04))
hist(mice_output$Age, freq=F, main='Age: MICE Output',
     col='lightgreen', ylim=c(0,0.04))
```")

(note-as-hiccup
 (plotting-function->svg
  (fn []
    (par :mfrow [1 2])
    (-> titanic
        ($ 'Age)
        (hist :freq 'F
              :main "Age: Original Data"
              :col "darkred"
              :lim [0 0.04]
              :xlab "Age"))
    (-> mice-output
        ($ 'Age)
        (hist :freq 'F
              :main "Age: MICE Output"
              :col "lightgreen"
              :lim [0 0.04]
              :xlab "Age")))))

(note-md "Tripathi:
Things look good, so let's replace our age vector in the original data
with the output from the mice model.

Replace Age variable from the mice model:
```
# Original code:
titanic$Age <- mice_output$Age

```")

(note-void
 (def titanic
   ($<- titanic 'Age
        ($ mice-output 'Age))))

(note-md "Show new number of missing Age values
```
# Original code:
sum(is.na(titanic$Age))
```")

(note-r
 "after"
 (-> titanic
     ($ 'Age)
     is-na
     sum))

(note-md "## Feature Enginnering: Part 2

Tripathi:
I will create a couple of new age-dependent variables: Child and Mother.
A child will simply be someone under 18 years of age and
a mother is a passenger who is 1) female, 2) is over 18, 3) has more
than 0 children and 4) does not have the title 'Miss'.

Relationship between age & survival: I include Sex since we know
it's a significant predictor.


```
# Original code:
ggplot(titanic[1:891,], aes(Age, fill = factor(Survived))) +
  geom_histogram() + facet_grid(.~Sex) + theme_few()
```")

(note-as-hiccup
   (-> titanic
       (bra (colon 1 891) (empty-symbol))
       (ggplot (aes 'Age :fill '(factor Survived)))
       (r+ (geom_histogram)
           (facet_grid '(tilde . Sex))
           (theme_few))
       ggplot->svg))


(note-md "Tripathi: Create the column Child, and indicate whether child or adult:
```
# Original code:
 titanic$Child[titanic$Age < 18] <- 'Child'
 titanic$Child[titanic$Age >= 18] <- 'Adult'
```")

(note-void
 (def titanic
   (-> titanic
       (bra<- (r< ($ titanic 'Age) 18) "Child"
              "Child")
       (bra<- (r>= ($ titanic 'Age) 18) "Child"
              "Adult"))))

(note-md "Show counts:
```
# Original code:
table(titanic$Child, titanic$Survived)
```")

(note-as-md
 (-> (print-table ($ titanic 'Child)
                  ($ titanic 'Survived))
     r-lines->md))

(note-md "Adding Mother variable:
```
# Original code:
titanic$Mother <- 'Not Mother'
titanic$Mother[titanic$Sex == 'female' & titanic$Parch >0 & titanic$Age > 18 & titanic$title != 'Miss'] <- 'Mother'
```")

(note-void
 (def titanic
   (-> titanic
       ($<- 'Mother
            "Not Mother")
       (bra<- (reduce r&
                      [(r== ($ titanic 'Sex) "female")
                       (r> ($ titanic 'Parch) 0)
                       (r> ($ titanic 'Age) 18)
                       (r!= ($ titanic 'title) "Miss")])
              "Mother"
              "Mother"))))

(note-md "Show counts:
```
# Original code:
table(titanic$Mother, titanic$Survived)
```")

(note-as-md
 (-> (print-table ($ titanic 'Mother)
                  ($ titanic 'Survived))
     r-lines->md))


(note-md "Factorizing variables:
```
# Original code:
titanic$Child <- factor(titanic$Child)
titanic$Mother <- factor(titanic$Mother)
titanic$Pclass <- factor(titanic$Pclass)
titanic$Sex <- factor(titanic$Sex)
titanic$Embarked <- factor(titanic$Embarked)
titanic$Survived <- factor(titanic$Survived)
titanic$title <- factor(titanic$title)
titanic$fsizeD <- factor(titanic$fsizeD)
```")

(note-void
 (def titanic
   (reduce (fn [data symbol]
             ($<- data symbol
                  (factor ($ data symbol))))
           titanic
           ['Child 'Mother 'Pclass 'Sex
            'Embarked 'Survived 'title 'fsizeD])))

(note-md "Check classes of all columns:")

(note-r
 (lapply titanic (r "class")))

(note-md "# Prediction

Split into training & test sets:
```
# Original code:
train <- titanic[1:891,]
test <- titanic[892:1309,]
```")

(note-void
 (def train
   (bra titanic (colon 1 891) (empty-symbol)))
 (def test
   (bra titanic (colon 892 1309) (empty-symbol))))

(note-md "Building the model:

Tripathi: We then build our model using randomForest on the training set.

Set a random seed:
```
# Original code:
set.seed(754)
```")

(note-void
 (set-seed 754))

(note-md "Tripathi:
Build the model (note: not all possible variables are used):

```
# Original code:
titanic_model <- randomForest(Survived ~ Pclass + Sex + Age + SibSp + Parch +
                                Fare + Embarked + title +
                                fsizeD + Child + Mother,
                              data = train)
```")

(note-void
 (def titanic-model
   (randomForest '(tilde Survived
                         (+ Pclass Sex Age SibSp Parch
                            Fare Embarked title
                            fsizeD Child Mother))
                 :data train)))

(note-md "Show model error:
```
# Original code:
plot(titanic_model, ylim=c(0,0.36))
legend('topright', colnames(titanic_model$err.rate), col=1:3, fill=1:3)
```")

(note-as-hiccup
 (plotting-function->svg
  (fn []
    (plot titanic-model :ylim [0 0.36]
          :main "Model Error")
    (legend "topright"
            (colnames ($ titanic-model 'err.rate))
            :col (colon 1 3)
            :fill (colon 1 3)))))


(note-md "Tripathi:
The black line shows the overall error rate which falls below 20%.
The red and green lines show the error rate for 'died' and 'survived',
respectively. We can see that right now we're much more successful predicting
death than we are survival.")

(note-md "## Variable Importance
Get importance:
```
# Original code:
importance    <- importance(titanic_model)
varImportance <- data.frame(Variables = row.names(importance),
                            Importance = round(importance[ ,'MeanDecreaseGini'],2))
```")

(importance titanic-model)

(note-void
 (def importance-info
   (importance titanic-model))
 (def var-importance
   (data-frame :Variables (row-names importance-info)
               :Importance (-> importance-info
                               (bra (empty-symbol) "MeanDecreaseGini")
                               round))))

(note-r importance-info)

(note-r var-importance)

(note-md "## Variable importance

Create a rank variable based on importance:
```
# Original code:
rankImportance <- varImportance %>%
   mutate(Rank = paste0('#',dense_rank(desc(Importance))))
```")

(note-void
 (def rank-importance
   (-> var-importance
       (mutate :Rank '(paste0 "#" (dense_rank (desc Importance)))))))

(note-r rank-importance)

(note-md "Tripathi: Use ggplot2 to visualize the relative importance of variables

```
# Original code:
ggplot(rankImportance, aes(x = reorder(Variables, Importance),
                           y = Importance, fill = Importance)) +
  geom_bar(stat='identity') +
  geom_text(aes(x = Variables, y = 0.5, label = Rank),
            hjust=0, vjust=0.55, size = 4, colour = 'red') +
  labs(x = 'Variables') +
  coord_flip() +
  theme_few()
```")

(note-as-hiccup
 (-> rank-importance
     (ggplot (aes :x '(reorder Variables Importance)
                  :y 'Importance
                  :fill 'Importance))
     (r+ (geom_bar :stat "Identity")
         (geom_text (aes :x 'Variables
                         :y 0.5
                         :label 'Rank)
                    :hjust 0
                    :vjust 0.55
                    :size 4
                    :colour "red")
         (labs :x "Variables")
         (coord_flip)
         (theme_few))
     ggplot->svg))

(note-md "Tripathi:
From the plot we can see that the 'title' variable has the highest
relative importance out of all of our predictor variables.")

(note-md "## Final Prediction

Predict using the test set:
```
# Original code:
prediction <- predict(titanic_model, test)
prediction
```")

(note-r
 (def prediction
   (predict titanic-model test)))

(note-md "Tripathi:
Save the solution to a dataframe with two columns: PassengerId and Survived (prediction).
```
# Original code:
Output<- data.frame(PassengerID = test$PassengerId, Survived = prediction)
Output
```")

(note-r
 (def output (data-frame :PassengerId ($ test 'PassengerId)
                         :Survived prediction)))

(note-md "Write the Output to file:
```
# Original code:
write.csv(Output, file = 'pradeep_titanic_output.csv', row.names = F)
```")

(note-void
 (write-csv output
            :file "/tmp/pradeep_titanic_output.csv"
            :row.names 'F))


(note-md "Tripathi:
Conclusion: Thank you for taking the time to read through my first
exploration of a Titanic Kaggle dataset. Again, this newbie welcomes comments and suggestions!")

(note/render-this-ns!)

